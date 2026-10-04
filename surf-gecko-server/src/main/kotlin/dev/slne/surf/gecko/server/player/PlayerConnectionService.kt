package dev.slne.surf.gecko.server.player

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent

object PlayerConnectionService : EventRegistrar {
    private val connectionLogger: ComponentLogger = ComponentLogger.logger("ConnectionService")

    override fun register(node: EventNode<Event>) {
        node.addListener(AsyncPlayerConfigurationEvent::class.java, ::handleConnection)
        node.addListener(PlayerDisconnectEvent::class.java, ::handleDisconnection)
    }

    private fun handleConnection(event: AsyncPlayerConfigurationEvent) {
        if (!event.isFirstConfig) return

        connectionLogger.info("${event.player.username} (${event.player.uuid}) connected from ${event.player.playerConnection.remoteAddress} on P${event.player.playerConnection.protocolVersion}")
    }

    private fun handleDisconnection(event: PlayerDisconnectEvent) {
        PlayerLanguages.invalidate(event.player.uuid)
        connectionLogger.info("${event.player.username} (${event.player.uuid}) disconnected")
    }
}
