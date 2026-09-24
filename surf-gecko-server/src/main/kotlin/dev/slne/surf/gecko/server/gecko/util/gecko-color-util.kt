package dev.slne.surf.gecko.server.gecko.util

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

val GECKO_PRIMARY = TextColor.fromHexString("#5EA3C0")!!
val GECKO_SECONDARY = TextColor.fromHexString("#B9D9DC")!!
val GECKO_HIGHLIGHT = TextColor.fromHexString("#FCC500")!!
val GECKO_USELESS = TextColor.fromHexString("#DBEBE2")!!

fun SurfComponentBuilder.appendPrefix() = append {
    append(BitmapProvider.translateToComponent("Gecko", Colors.WHITE, GECKO_PRIMARY))
    appendSpace()
    //    spacer("»")
    //    appendSpace()
    //    note("CC")
    //    appendSpace()
    //    darkSpacer("|")
    //    appendSpace()
}

fun SurfComponentBuilder.geckoPrimary(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_PRIMARY, *decoration)

fun SurfComponentBuilder.geckoSecondary(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_SECONDARY, *decoration)

fun SurfComponentBuilder.geckoHighlight(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_HIGHLIGHT, *decoration)

fun SurfComponentBuilder.geckoUseless(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_USELESS, *decoration)

fun Component.removeItalics() = decoration(TextDecoration.ITALIC, false)
fun Iterable<Component>.removeItalics() = map { it.removeItalics() }