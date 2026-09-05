package dev.slne.surf.gecko.map.creator.export

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object PasteService {
    private const val POST_URL = "https://api.pastes.dev/post"
    private const val VIEW_URL = "https://pastes.dev/"
    private const val CONTENT_TYPE = "text/kotlin"
    private const val USER_AGENT = "surf-gecko-map-creator (https://slne.dev)"

    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    suspend fun upload(content: String): String = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder(URI.create(POST_URL))
            .header("Content-Type", CONTENT_TYPE)
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val status = response.statusCode()

        check(status in 200..299) { "pastes.dev responded with status $status: ${response.body()}" }

        val key = response.headers().firstValue("Location").orElse(null)?.takeIf { it.isNotBlank() }
            ?: keyFromBody(response.body())

        VIEW_URL + key
    }

    private fun keyFromBody(body: String): String {
        val key = runCatching {
            JsonParser.parseString(body).asJsonObject.get("key")?.asString
        }.getOrNull()

        return key?.takeIf { it.isNotBlank() }
            ?: error("pastes.dev did not return a paste key: $body")
    }
}
