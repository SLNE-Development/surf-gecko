package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextColor

val COLOR_SEEKER = TextColor.color(227, 0, 58)
val COLOR_HIDER = Colors.INFO
val COLOR_SPECTATOR = Colors.SPACER

enum class GeckoGameRole(
    val id: String,
    val color: TextColor,
    display: SurfComponentBuilder.() -> Unit
) {
    SEEKER("seeker", COLOR_SEEKER, {
        text("Sucher", COLOR_SEEKER)
    }),
    HIDER("hider", COLOR_HIDER, {
        text("Verstecker", COLOR_HIDER)
    }),
    SPECTATOR("spectator", COLOR_SPECTATOR, {
        text("Zuschauer", COLOR_SPECTATOR)
    });

    val displayText = SurfComponentBuilder(display)
}