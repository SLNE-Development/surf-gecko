package dev.slne.surf.gecko.server.gecko.visual

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.showTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player

object ScreenFade {
    private val FILL = Component.text("")
        .font(key("sonar", "screen"))
        .shadowColor(ShadowColor.none())

    fun play(
        player: Player,
        fadeInTicks: Long,
        stayTicks: Long,
        fadeOutTicks: Long,
        color: TextColor = NamedTextColor.BLACK
    ) = player.showTitle {
        title = FILL.color(color)
        times {
            fadeIn(fadeInTicks)
            stay(stayTicks)
            fadeOut(fadeOutTicks)
        }
    }
}
