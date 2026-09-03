package dev.slne.surf.gecko.server.event

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.GlobalEventHandler

@Singleton
class GeckoEventService @Inject constructor(
    private val globalEventHandler: GlobalEventHandler,
    private val registrars: Set<@JvmSuppressWildcards EventRegistrar>,
) : GeckoService {

    private val node: EventNode<Event> = EventNode.all(NODE_NAME)

    override suspend fun start() {
        for (registrar in registrars) {
            registrar.register(node)
        }

        globalEventHandler.addChild(node)
    }

    override suspend fun stop() {
        globalEventHandler.removeChild(node)
    }

    private companion object {
        const val NODE_NAME = "surf-gecko:core"
    }
}
