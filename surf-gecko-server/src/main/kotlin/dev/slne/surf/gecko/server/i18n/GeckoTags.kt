package dev.slne.surf.gecko.server.i18n

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.player.game.COLOR_HIDER
import dev.slne.surf.gecko.server.gecko.player.game.COLOR_SEEKER
import dev.slne.surf.gecko.server.gecko.player.game.COLOR_SPECTATOR
import dev.slne.surf.gecko.server.gecko.util.GECKO_HIGHLIGHT
import dev.slne.surf.gecko.server.gecko.util.GECKO_PRIMARY
import dev.slne.surf.gecko.server.gecko.util.GECKO_SECONDARY
import dev.slne.surf.gecko.server.gecko.util.GECKO_USELESS
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.Modifying
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object GeckoTags {
    private val gecko = BitmapProvider.translateToComponent("Gecko", Colors.WHITE, GECKO_PRIMARY)
        .append(Component.space())

    private val smallCaps = Modifying { current, _ ->
        if (current is TextComponent) {
            Component.text(current.content().toSmallCaps(), current.style())
        } else {
            current.children(emptyList())
        }
    }

    val resolver: TagResolver = TagResolver.builder()
        .color("primary", GECKO_PRIMARY)
        .color("secondary", GECKO_SECONDARY)
        .color("highlight", GECKO_HIGHLIGHT)
        .color("muted", GECKO_USELESS)
        .color("info", Colors.INFO)
        .color("note", Colors.NOTE)
        .color("success", Colors.SUCCESS)
        .color("warning", Colors.WARNING)
        .color("error", Colors.ERROR)
        .color("var", Colors.VARIABLE_VALUE)
        .color("var_key", Colors.VARIABLE_KEY)
        .color("spacer", Colors.SPACER)
        .color("dark_spacer", Colors.DARK_SPACER)
        .color("seeker", COLOR_SEEKER)
        .color("hider", COLOR_HIDER)
        .color("spectator", COLOR_SPECTATOR)
        .inserting("prefix", gecko)
        .inserting("info_prefix", Colors.INFO_PREFIX.append(Component.space()))
        .inserting("success_prefix", Colors.SUCCESS_PREFIX.append(Component.space()))
        .inserting("warning_prefix", Colors.WARNING_PREFIX.append(Component.space()))
        .inserting("error_prefix", Colors.ERROR_PREFIX.append(Component.space()))
        .tag("small_caps") { _, _ -> smallCaps }
        .build()

    private fun TagResolver.Builder.color(name: String, color: TextColor) =
        tag(name, Tag.styling(color))

    private fun TagResolver.Builder.inserting(name: String, component: Component) =
        tag(name, Tag.selfClosingInserting(component))
}
