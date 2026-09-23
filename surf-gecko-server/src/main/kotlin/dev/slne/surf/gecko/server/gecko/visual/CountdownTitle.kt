package dev.slne.surf.gecko.server.gecko.visual

import dev.slne.surf.api.core.messages.adventure.showTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player

object CountdownTitle {
    enum class Palette { NUMBER, GO }

    fun show(
        player: Player,
        text: String,
        subtitle: Component = Component.empty(),
        palette: Palette = Palette.NUMBER
    ) {
        val start = (player.instance ?: return).shaderTick(2) / 2
        val green = 0x80 or (palette.ordinal shl 6) or ((start shr 8) and 0x3F)
        val blue = start.low

        player.showTitle {
            title = Component.text(text, TextColor.color(0xFD, green, blue), TextDecoration.BOLD)
                .shadowColor(ShadowColor.shadowColor(TextColor.color(0xFC, green, blue), 0xFF))
            this.subtitle = subtitle
            times {
                fadeIn(0)
                stay(16)
                fadeOut(4)
            }
        }
    }
}
