package dev.slne.surf.gecko.server.gecko

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.scoreboard.GeckoScoreboardManager
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent

@Singleton
class GeckoGameJoinService : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handleConfiguration)
        node.addListener(::handleSpawn)
        node.addListener(::handleDisconnect)
    }

    private fun handleConfiguration(event: AsyncPlayerConfigurationEvent) {
        event.spawningInstance = GeckoLobby.instance
        event.player.respawnPoint = GeckoLobby.spawn
    }

    private fun handleSpawn(event: PlayerSpawnEvent) {
        val game = GeckoGameManager.findGame(event.player.uuid)

        if (game == null) {
            GeckoScoreboardManager.hideSidebar(event.player)
            return
        }

        GeckoScoreboardManager.showSidebar(game, event.player)
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        GeckoScoreboardManager.hideSidebar(event.player)
        GeckoGameManager.releasePlayer(event.player.uuid)
    }
}
