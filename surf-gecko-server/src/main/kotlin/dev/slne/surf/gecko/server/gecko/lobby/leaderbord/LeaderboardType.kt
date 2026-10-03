package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import net.minestom.server.coordinate.Pos

enum class LeaderboardType(val id: String, val position: Pos) {
    WINS("wins", Pos(-14.5, 145.5, 3.5, 160f, 0f)),
    GAMES_PLAYED("games-played", Pos(-19.5, 145.5, 4.5, 180f, 0f))
}
