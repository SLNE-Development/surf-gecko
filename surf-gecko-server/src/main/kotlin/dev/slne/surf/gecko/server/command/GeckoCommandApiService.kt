package dev.slne.surf.gecko.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.command.platform.MinestomCommandAPIService
import dev.slne.surf.gecko.command.platform.MinestomCommandOwnership
import dev.slne.surf.gecko.command.platform.MinestomSuggestionListener
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

@Singleton
class GeckoCommandApiService @Inject constructor(
    ownership: MinestomCommandOwnership,
) : GeckoService, EventRegistrar {

    private val platform = MinestomCommandAPIService(ownership)
    private val suggestions = MinestomSuggestionListener(ownership)

    override suspend fun start() = platform.start()

    override suspend fun stop() = platform.stop()

    override fun register(node: EventNode<Event>) = suggestions.register(node)
}
