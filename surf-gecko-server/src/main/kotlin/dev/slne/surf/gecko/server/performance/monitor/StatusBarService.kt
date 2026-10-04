package dev.slne.surf.gecko.server.performance.monitor

import dev.slne.surf.api.minestom.event.EventRegistrar
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.server.ServerTickMonitorEvent

object StatusBarService : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(ServerTickMonitorEvent::class.java) { event ->
            TickStatistics.record(event.tickMonitor.tickTime)
        }
    }

    fun start() {
        StatusBarManager.init()
    }

    fun stop() {
        StatusBarManager.shutdown()
    }
}
