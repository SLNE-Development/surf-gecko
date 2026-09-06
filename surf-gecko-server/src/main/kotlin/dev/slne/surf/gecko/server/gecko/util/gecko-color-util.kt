package dev.slne.surf.gecko.server.gecko.util

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

val GECKO_PRIMARY = TextColor.fromHexString("#5EA3C0")!!
val GECKO_SECONDARY = TextColor.fromHexString("#B9D9DC")!!
val GECKO_HIGHLIGHT = TextColor.fromHexString("#FCC500")!!
val GECKO_USELESS = TextColor.fromHexString("#DBEBE2")!!

fun SurfComponentBuilder.appendPrefix() = append(Component.empty())

fun SurfComponentBuilder.geckoPrimary(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_PRIMARY, *decoration)

fun SurfComponentBuilder.geckoSecondary(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_SECONDARY, *decoration)

fun SurfComponentBuilder.geckoHighlight(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_HIGHLIGHT, *decoration)

fun SurfComponentBuilder.geckoUseless(text: String, vararg decoration: TextDecoration) =
    text(text, GECKO_USELESS, *decoration)