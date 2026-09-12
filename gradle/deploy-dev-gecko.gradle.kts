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
import java.util.UUID

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

    @TaskAction
    fun deploy() {
        val source = jar.get().asFile
        val directory = remoteDirectory.get()
        val fileName = remoteFileName.get()

        val client = PterodactylClient(
            panelUrl.get(),
            apiKey.get(),
            serverId.get()
        )

        logger.lifecycle(
            "Uploading ${source.name} (${source.length() / 1024 / 1024} MiB) to ${serverId.get()}:$directory"
        )
        val uploadedAs = client.upload(source, directory)

        if (uploadedAs != fileName) {
            logger.lifecycle("Replacing $directory$fileName")
            client.delete(directory, fileName)
            client.rename(directory, uploadedAs, fileName)
        }

        logger.lifecycle("Sending power signal '${powerSignal.get()}'")
        client.power(powerSignal.get())

        logger.lifecycle("Deployed $fileName to server ${serverId.get()}")
    }
}

class PterodactylClient(panelUrl: String, private val apiKey: String, serverId: String) {

    private val base = "${panelUrl.trimEnd('/')}/api/client/servers/$serverId"

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun upload(source: File, directory: String): String {
        val signed = send(request(URI.create("$base/files/upload")).GET().build())

        @Suppress("UNCHECKED_CAST")
        val parsed = JsonSlurper().parseText(signed.body()) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val attributes = parsed["attributes"] as Map<String, Any>
        val uploadUrl = attributes["url"] as String

        val separator = if (uploadUrl.contains('?')) "&" else "?"
        val target = URI.create(
            uploadUrl + separator + "directory=" + URLEncoder.encode(directory, StandardCharsets.UTF_8)
        )

        val boundary = "gecko-" + UUID.randomUUID()
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
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofFile(payload))
                    .build()
            )
        } finally {
            Files.deleteIfExists(payload)
        }

        return source.name
    }

    fun delete(directory: String, fileName: String) {
        send(
            json("$base/files/delete", "POST", """{"root":"$directory","files":["$fileName"]}"""),
            allowed = setOf(404)
        )
    }

    fun rename(directory: String, from: String, to: String) {
        send(
            json(
                "$base/files/rename",
                "PUT",
                """{"root":"$directory","files":[{"from":"$from","to":"$to"}]}"""
            )
        )
    }

    fun power(signal: String) {
        send(json("$base/power", "POST", """{"signal":"$signal"}"""))
    }

    private fun request(uri: URI): HttpRequest.Builder = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofMinutes(15))
        .header("Authorization", "Bearer $apiKey")
        .header("Accept", "application/json")

    private fun json(url: String, method: String, body: String): HttpRequest =
        request(URI.create(url))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build()

    private fun send(request: HttpRequest, allowed: Set<Int> = emptySet()): HttpResponse<String> {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        val status = response.statusCode()

        if (status !in 200..299 && status !in allowed) {
            throw GradleException(
                "Pterodactyl request to ${request.uri()} failed with $status: ${response.body()}"
            )
        }
        return response
    }

    private fun OutputStream.ascii(value: String) = write(value.toByteArray(StandardCharsets.UTF_8))
}

val shadowJarTask = tasks.named<Jar>("shadowJar")

tasks.register<DeployDevGeckoTask>("deployDevGecko") {
    group = "deployment"
    description = "Builds the shadow jar and deploys it to the dev Pterodactyl server from .env"

    notCompatibleWithConfigurationCache("Deployment task talks to the Pterodactyl API at execution time")

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
