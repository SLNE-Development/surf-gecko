package dev.slne.surf.gecko.server.gecko

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent

@Singleton
class GeckoGameJoinService : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handleLogin)
        node.addListener(::handleConfiguration)
        node.addListener(::handleDisconnect)
    }

    private fun handleLogin(event: PlayerLoginEvent) {
        if (!event.isAllowed) return

        if (GeckoGameManager.reserveLobbySlot(event.player.uuid) == null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, NO_GAME_AVAILABLE)
        }
    }

    private fun handleConfiguration(event: AsyncPlayerConfigurationEvent) {
        val game = GeckoGameManager.findGame(event.player.uuid) ?: return

        event.spawningInstance = game.instance
        event.player.respawnPoint = game.settings.map.mapLocations.lobbySpawn
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        GeckoGameManager.releasePlayer(event.player.uuid)
    }

    private companion object {
        val NO_GAME_AVAILABLE: Component =
            text("Aktuell ist keine Runde verfügbar.", NamedTextColor.RED)
    }
}
