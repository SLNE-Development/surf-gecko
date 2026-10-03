package dev.slne.surf.gecko.server.i18n

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import dev.slne.surf.gecko.server.config.Config
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.*

class GeckoTranslationLoader(private val config: Config.TranslationsConfig) {
    private val logger = ComponentLogger.logger("GeckoTranslations")
    private val cacheDirectory = Path(config.cacheDirectory)
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    enum class Source { GITHUB, CACHE, BUNDLED }

    data class Result(
        val source: Source,
        val translations: Map<GeckoLanguage, Map<String, List<String>>>,
    )

    fun load(): Result {
        val fetched = runCatching { fetchFromGitHub() }
            .onFailure { logger.warn("Could not fetch translations from GitHub: {}", it.toString()) }
            .getOrNull()

        if (fetched != null) {
            runCatching { writeCache(fetched) }
                .onFailure { logger.warn("Could not cache translations", it) }
            return Result(Source.GITHUB, parseAll(fetched))
        }

        val cached = runCatching { readCache() }
            .onFailure { logger.warn("Could not read cached translations", it) }
            .getOrNull()

        if (!cached.isNullOrEmpty()) {
            return Result(Source.CACHE, parseAll(cached))
        }

        return Result(Source.BUNDLED, parseAll(readBundled()))
    }

    private fun fetchFromGitHub(): Map<GeckoLanguage, Map<String, String>> =
        GeckoLanguage.entries.associateWith { language ->
            val directory = "${config.path.trim('/')}/${language.id}"
            val listing = JsonParser.parseString(request(contentsUrl(directory), raw = false)).asJsonArray

            listing.map { it.asJsonObject }
                .filter { it["type"].asString == "file" && it["name"].asString.endsWith(".json") }
                .associate { file ->
                    val name = file["name"].asString
                    name to request(contentsUrl("$directory/$name"), raw = true)
                }
        }

    private fun contentsUrl(path: String): URI {
        val encodedPath = path.split('/').joinToString("/") {
            URLEncoder.encode(it, StandardCharsets.UTF_8).replace("+", "%20")
        }
        val ref = URLEncoder.encode(config.branch, StandardCharsets.UTF_8)
        return URI.create("https://api.github.com/repos/${config.repository}/contents/$encodedPath?ref=$ref")
    }

    private fun request(uri: URI, raw: Boolean): String {
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", if (raw) "application/vnd.github.raw+json" else "application/vnd.github+json")
            .header("User-Agent", "surf-gecko")
            .apply { if (config.token.isNotBlank()) header("Authorization", "Bearer ${config.token}") }
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        check(response.statusCode() == 200) { "GET $uri returned ${response.statusCode()}" }
        return response.body()
    }

    private fun writeCache(files: Map<GeckoLanguage, Map<String, String>>) {
        for ((language, languageFiles) in files) {
            val directory = cacheDirectory.resolve(language.id)
            if (directory.exists()) {
                directory.listDirectoryEntries("*.json").forEach { it.deleteIfExists() }
            }
            directory.createDirectories()

            for ((name, content) in languageFiles) {
                directory.resolve(name).writeText(content)
            }
        }
    }

    private fun readCache(): Map<GeckoLanguage, Map<String, String>> =
        GeckoLanguage.entries.associateWith { language ->
            val directory = cacheDirectory.resolve(language.id)
            if (!directory.isDirectory()) return@associateWith emptyMap()

            directory.listDirectoryEntries("*.json").associate { it.name to it.readText() }
        }.filterValues { it.isNotEmpty() }

    private fun readBundled(): Map<GeckoLanguage, Map<String, String>> {
        val index = resource("lang/index.txt")?.lines().orEmpty()
            .map(String::trim)
            .filter(String::isNotEmpty)

        return GeckoLanguage.entries.associateWith { language ->
            index.filter { it.startsWith("${language.id}/") }
                .associate { path ->
                    Path(path).name to (resource("lang/$path") ?: error("Missing bundled translation $path"))
                }
        }
    }

    private fun resource(path: String): String? =
        javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }

    private fun parseAll(files: Map<GeckoLanguage, Map<String, String>>) =
        files.mapValues { (language, languageFiles) ->
            buildMap {
                for ((name, content) in languageFiles.toSortedMap()) {
                    runCatching { flatten("", JsonParser.parseString(content), this) }
                        .onFailure { logger.error("Could not parse translation file {}/{}", language.id, name, it) }
                }
            }
        }

    private fun flatten(prefix: String, element: JsonElement, into: MutableMap<String, List<String>>) {
        when {
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                flatten(if (prefix.isEmpty()) key else "$prefix.$key", value, into)
            }

            element.isJsonArray -> into[prefix] = element.asJsonArray.map { it.asString }
            element.isJsonPrimitive -> into[prefix] = listOf(element.asString)
        }
    }
}
