package dev.slne.surf.gecko.server.player

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import dev.slne.surf.gecko.server.player.config.AwaitSettingsTask
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import java.util.logging.Logger

@Singleton
class PlayerConnectionService : EventRegistrar {
    private val connectionLogger: Logger = Logger.getLogger("PlayerConnectionService")

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerLoginEvent::class.java, ::handleConnection)
        node.addListener(PlayerDisconnectEvent::class.java, ::handleDisconnection)
        node.addListener(AwaitSettingsTask::handleSettingsChange)
    }

    private fun handleConnection(event: PlayerLoginEvent) {
        connectionLogger.info("Player ${event.player.username} connected with UUID ${event.player.uuid}")
    }

    private fun handleDisconnection(event: PlayerDisconnectEvent) {
        AwaitSettingsTask.handleDisconnect(event)

        connectionLogger.info("Player ${event.player.username} disconnected with UUID ${event.player.uuid}")
    }
}
