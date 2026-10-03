package dev.slne.surf.gecko.server.i18n

import dev.slne.surf.gecko.server.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.EnumMap

fun interface LocalizedComponent {
    fun render(language: GeckoLanguage): Component
}

fun translatable(key: String, vararg args: Pair<String, Any?>) =
    LocalizedComponent { GeckoTranslations.render(it, key, *args) }

object GeckoTranslations {
    private val logger = ComponentLogger.logger("GeckoTranslations")
    private val miniMessage = MiniMessage.builder()
        .editTags { it.resolver(GeckoTags.resolver) }
        .build()
    private val reloadMutex = Mutex()

    @Volatile
    private var translations: Map<GeckoLanguage, Map<String, List<String>>> = emptyMap()

    private lateinit var loader: GeckoTranslationLoader

    fun configure(config: Config.TranslationsConfig) {
        loader = GeckoTranslationLoader(config)
    }

    suspend fun reload(): GeckoTranslationLoader.Result = reloadMutex.withLock {
        val result = withContext(Dispatchers.IO) { loader.load() }
        translations = EnumMap<GeckoLanguage, Map<String, List<String>>>(GeckoLanguage::class.java)
            .apply { putAll(result.translations) }

        logger.info(
            "Loaded translations from {}: {}",
            result.source,
            result.translations.entries.joinToString { (language, keys) -> "${language.id}=${keys.size}" }
        )

        for (language in GeckoLanguage.entries) {
            val missing = missingKeys(language)
            if (missing.isNotEmpty()) {
                logger.warn("{} is missing {} key(s): {}", language.id, missing.size, missing.take(20))
            }
        }

        result
    }

    fun has(key: String) = translations.values.any { key in it }

    fun keyCount(language: GeckoLanguage) = translations[language]?.size ?: 0

    fun missingKeys(language: GeckoLanguage): Set<String> {
        val all = translations.values.flatMapTo(mutableSetOf()) { it.keys }
        return all - translations[language]?.keys.orEmpty()
    }

    private fun lines(language: GeckoLanguage, key: String): List<String> =
        translations[language]?.get(key)
            ?: translations[GeckoLanguage.FALLBACK]?.get(key)
            ?: listOf(key)

    fun render(language: GeckoLanguage, key: String, vararg args: Pair<String, Any?>): Component =
        deserialize(lines(language, key).joinToString("\n"), language, args)

    fun renderLines(language: GeckoLanguage, key: String, vararg args: Pair<String, Any?>): List<Component> =
        lines(language, key).map { deserialize(it, language, args) }

    fun renderPlain(language: GeckoLanguage, key: String, vararg args: Pair<String, Any?>): String =
        PlainTextComponentSerializer.plainText().serialize(render(language, key, *args))

    private fun deserialize(input: String, language: GeckoLanguage, args: Array<out Pair<String, Any?>>): Component {
        if (args.isEmpty()) return miniMessage.deserialize(input)

        val resolver = TagResolver.resolver(args.map { (name, value) -> placeholder(name, value, language) })
        return miniMessage.deserialize(input, resolver)
    }

    private fun placeholder(name: String, value: Any?, language: GeckoLanguage): TagResolver = when (value) {
        is LocalizedComponent -> Placeholder.component(name, value.render(language))
        is ComponentLike -> Placeholder.component(name, value)
        else -> Placeholder.unparsed(name, value.toString())
    }
}
