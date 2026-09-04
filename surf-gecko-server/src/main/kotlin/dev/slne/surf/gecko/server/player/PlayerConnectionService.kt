package dev.slne.surf.gecko.server.player

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import dev.slne.surf.gecko.server.player.config.AwaitSettingsTask
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent

@Singleton
class PlayerConnectionService : EventRegistrar {
    private val connectionLogger: ComponentLogger = ComponentLogger.logger("ConnectionService")

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerLoginEvent::class.java, ::handleConnection)
        node.addListener(PlayerDisconnectEvent::class.java, ::handleDisconnection)
        node.addListener(AwaitSettingsTask::handleSettingsChange)
    }

    private fun handleConnection(event: PlayerLoginEvent) {
        connectionLogger.info("Player ${event.player.username} (${event.player.uuid}) connected from ${event.player.playerConnection.remoteAddress} on P${event.player.playerConnection.protocolVersion}")
    }

    private fun handleDisconnection(event: PlayerDisconnectEvent) {
        AwaitSettingsTask.handleDisconnect(event)
        connectionLogger.info("Player ${event.player.username} (${event.player.uuid}) disconnected")
    }
}
