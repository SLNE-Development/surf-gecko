package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import dev.slne.surf.gecko.server.event.register
import net.minestom.server.coordinate.Pos

object LeaderboardManager {
    private val killsLeaderboardPos = Pos(-19.5, 145.0, -8.5, 90f, 0f)
    private val winsLeaderboardPos = Pos(-19.5, 145.0, -8.5, 90f, 0f)

    suspend fun init() {
        LeaderboardListener.register()
    }
}