package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import net.minestom.server.coordinate.Pos

enum class LeaderboardType(val title: String, val position: Pos) {
    WINS("Siege", Pos(-14.5, 146.0, 3.5, 160f, 0f)),
    GAMES_PLAYED("Gespielte Spiele", Pos(-19.5, 146.0, 4.5, 180f, 0f))
}
