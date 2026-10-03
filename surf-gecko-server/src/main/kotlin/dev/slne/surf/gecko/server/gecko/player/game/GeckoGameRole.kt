package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.gecko.server.i18n.translatable
import net.kyori.adventure.text.format.TextColor

val COLOR_SEEKER = TextColor.color(227, 0, 58)
val COLOR_HIDER = Colors.INFO
val COLOR_SPECTATOR = Colors.SPACER

enum class GeckoGameRole(
    val id: String,
    val color: TextColor,
) {
    SEEKER("seeker", COLOR_SEEKER),
    HIDER("hider", COLOR_HIDER),
    SPECTATOR("spectator", COLOR_SPECTATOR);

    val displayText = translatable("role.$id.name")
    val description = translatable("role.$id.description")
}
