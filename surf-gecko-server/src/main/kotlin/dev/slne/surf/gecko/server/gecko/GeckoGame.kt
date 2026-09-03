package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import net.minestom.server.MinecraftServer

class GeckoGame(
    val internalId: ULong,
    val settings: GeckoGameSettings
) {
    var state: GeckoGameState = GeckoGameState.OFFLINE

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()


    val players
        get() = lobbyPlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        } + gamePlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        }
}
