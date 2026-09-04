package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Instance

class GeckoGame(
    val internalId: ULong,
    val settings: GeckoGameSettings,
    val instance: Instance
) {
    var state: GeckoGameState = GeckoGameState.OFFLINE

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = lobbyPlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        } + gamePlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        }
}
