package dev.slne.surf.gecko.server.event

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.GlobalEventHandler

/**
 * Attaches every extension's listeners to one child node of the global handler.
 *
 * Extensions never touch the global handler themselves - they declare an [EventRegistrar] and get
 * handed this node, so shutdown detaches all of them by removing the single child.
 */
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
