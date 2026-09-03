package dev.slne.surf.gecko.server.gecko.state

import net.minestom.server.entity.Player

enum class GeckoGameState {
    OFFLINE,
    PREPARING,
    LOBBY,
    STARTING,
    GAME,
    ENDING,
    ENDED;

    fun acceptPlayers(player: Player) = this in listOf(LOBBY, STARTING)
}