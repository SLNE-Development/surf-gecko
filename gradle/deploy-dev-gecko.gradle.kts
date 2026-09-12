import groovy.json.JsonSlurper
import java.io.OutputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Duration

fun parseDotEnv(text: String): Map<String, String> = text.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .map { it.removePrefix("export ").trim() }
    .mapNotNull { line ->
        val separator = line.indexOf('=')
        if (separator <= 0) return@mapNotNull null

        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        key to value
    }
    .toMap()

val dotEnvText = providers.fileContents(
    rootProject.layout.projectDirectory.file(".env")
).asText

fun dotEnv(key: String): Provider<String> = dotEnvText
    .map { parseDotEnv(it)[key].orEmpty() }
    .filter { it.isNotEmpty() }
    .orElse(providers.environmentVariable(key))

abstract class DeployDevGeckoTask : DefaultTask() {

    @get:InputFile
    abstract val jar: RegularFileProperty

    @get:Internal
    abstract val panelUrl: Property<String>

    @get:Internal
    abstract val apiKey: Property<String>

    @get:Internal
    abstract val serverId: Property<String>

    @get:Internal
    abstract val remoteDirectory: Property<String>

    @get:Internal
    abstract val remoteFileName: Property<String>

    @get:Internal
    abstract val powerSignal: Property<String>

    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    @TaskAction
    fun deploy() {
        val source = jar.get().asFile
        val directory = remoteDirectory.get()
        val fileName = remoteFileName.get()

        logger.lifecycle("Uploading ${source.name} (${source.length() / 1024 / 1024} MiB) to ${serverId.get()}:$directory")
        val uploadedAs = upload(source, directory)

        if (uploadedAs != fileName) {
            logger.lifecycle("Replacing $directory$fileName")
            deleteRemote(directory, fileName)
            rename(directory, uploadedAs, fileName)
        }

        logger.lifecycle("Sending power signal '${powerSignal.get()}'")
        power(powerSignal.get())

        logger.lifecycle("Deployed $fileName to server ${serverId.get()}")
    }

    private fun clientApi(path: String): URI {
        val base = panelUrl.get().trimEnd('/')
        return URI.create("$base/api/client/servers/${serverId.get()}$path")
    }

    private fun request(uri: URI): HttpRequest.Builder = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofMinutes(15))
        .header("Authorization", "Bearer ${apiKey.get()}")
        .header("Accept", "application/json")

    private fun send(request: HttpRequest, allowed: IntRange = 200..299): HttpResponse<String> {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in allowed) {
            throw GradleException(
                "Pterodactyl request to ${request.uri()} failed with ${response.statusCode()}: ${response.body()}"
            )
        }
        return response
    }

    private fun json(uri: URI, method: String, body: String) = send(
        request(uri)
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build()
    )

    private fun upload(source: File, directory: String): String {
        val signed = send(request(clientApi("/files/upload")).GET().build())

        @Suppress("UNCHECKED_CAST")
        val parsed = JsonSlurper().parseText(signed.body()) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val attributes = parsed["attributes"] as Map<String, Any>
        val uploadUrl = attributes["url"] as String

        val separator = if (uploadUrl.contains('?')) "&" else "?"
        val target = URI.create(
            uploadUrl + separator + "directory=" + URLEncoder.encode(directory, StandardCharsets.UTF_8)
        )

        val boundary = "gecko-" + java.util.UUID.randomUUID()
        val payload = Files.createTempFile("gecko-upload", ".multipart")

        try {
            Files.newOutputStream(payload, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
                out.ascii("--$boundary\r\n")
                out.ascii("Content-Disposition: form-data; name=\"files\"; filename=\"${source.name}\"\r\n")
                out.ascii("Content-Type: application/java-archive\r\n\r\n")
                source.inputStream().use { it.copyTo(out) }
                out.ascii("\r\n--$boundary--\r\n")
            }

            send(
                HttpRequest.newBuilder(target)
                    .timeout(Duration.ofMinutes(30))
                    .header("Authorization", "Bearer ${apiKey.get()}")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofFile(payload))
                    .build()
            )
        } finally {
            Files.deleteIfExists(payload)
        }

        return source.name
    }

    private fun OutputStream.ascii(value: String) = write(value.toByteArray(StandardCharsets.UTF_8))

    private fun deleteRemote(directory: String, fileName: String) {
        val response = http.send(
            request(clientApi("/files/delete"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """{"root":"$directory","files":["$fileName"]}"""
                    )
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() !in 200..299 && response.statusCode() != 404) {
            throw GradleException(
                "Could not delete $directory$fileName: ${response.statusCode()} ${response.body()}"
            )
        }
    }

    private fun rename(directory: String, from: String, to: String) = json(
        clientApi("/files/rename"),
        "PUT",
        """{"root":"$directory","files":[{"from":"$from","to":"$to"}]}"""
    )

    private fun power(signal: String) = json(
        clientApi("/power"),
        "POST",
        """{"signal":"$signal"}"""
    )
}

val shadowJarTask = tasks.named<Jar>("shadowJar")

tasks.register<DeployDevGeckoTask>("deployDevGecko") {
    group = "deployment"
    description = "Builds the shadow jar and deploys it to the dev Pterodactyl server from .env"

    dependsOn(shadowJarTask)
    jar.set(shadowJarTask.flatMap { it.archiveFile })

    panelUrl.set(dotEnv("PTERODACTYL_URL"))
    apiKey.set(dotEnv("PTERODACTYL_API_KEY"))
    serverId.set(dotEnv("PTERODACTYL_SERVER_ID"))
    remoteDirectory.set(dotEnv("PTERODACTYL_DIRECTORY").orElse("/"))
    remoteFileName.set(dotEnv("PTERODACTYL_TARGET_FILE").orElse("gecko-server.jar"))
    powerSignal.set(dotEnv("PTERODACTYL_POWER_SIGNAL").orElse("restart"))

    doFirst {
        listOf(
            "PTERODACTYL_URL" to panelUrl,
            "PTERODACTYL_API_KEY" to apiKey,
            "PTERODACTYL_SERVER_ID" to serverId
        ).forEach { (key, property) ->
            require(property.isPresent) {
                "$key is missing - add it to .env in the project root (see .env.example)"
            }
        }
    }
}
