package dev.slne.surf.gecko.server.i18n

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.minestom.server.entity.Player
import kotlin.time.Duration

val Audience.language: GeckoLanguage
    get() = if (this is Player) PlayerLanguages.get(this) else GeckoLanguage.FALLBACK

fun Audience.translate(key: String, vararg args: Pair<String, Any?>): Component =
    GeckoTranslations.render(language, key, *args)

fun Audience.translateLines(key: String, vararg args: Pair<String, Any?>): List<Component> =
    GeckoTranslations.renderLines(language, key, *args)

fun Audience.translatePlain(key: String, vararg args: Pair<String, Any?>): String =
    GeckoTranslations.renderPlain(language, key, *args)

fun Audience.render(component: LocalizedComponent): Component = component.render(language)

fun Audience.sendTranslated(key: String, vararg args: Pair<String, Any?>) =
    sendMessage(translate(key, *args))

fun Audience.sendTranslatedActionBar(key: String, vararg args: Pair<String, Any?>) =
    sendActionBar(translate(key, *args))

fun Iterable<Audience>.sendTranslated(key: String, vararg args: Pair<String, Any?>) =
    forEach { it.sendTranslated(key, *args) }

fun formatDuration(duration: Duration, short: Boolean = false) = LocalizedComponent { language ->
    val parts = listOf(
        "day" to duration.inWholeDays,
        "hour" to duration.inWholeHours % 24,
        "minute" to duration.inWholeMinutes % 60,
        "second" to duration.inWholeSeconds % 60,
    ).filter { it.second > 0 }.ifEmpty { listOf("second" to 0L) }

    Component.join(
        JoinConfiguration.separator(
            GeckoTranslations.render(language, "common.time.separator")
        ),
        parts.map { (unit, amount) ->
            val form = when {
                short -> "short"
                amount == 1L -> "one"
                else -> "other"
            }
            GeckoTranslations.render(language, "common.time.$unit.$form", "amount" to amount)
        }
    )
}
