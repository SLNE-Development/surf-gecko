package dev.slne.surf.gecko.server.performance.tps

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.server.ServerTickMonitorEvent

@Singleton
class TpsBarService : GeckoService, EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(ServerTickMonitorEvent::class.java) { event ->
            TickStatistics.record(event.tickMonitor.tickTime)
        }
    }

    override suspend fun start() {
        TpsBarManager.init()
    }

    override suspend fun stop() {
        TpsBarManager.shutdown()
    }
}
