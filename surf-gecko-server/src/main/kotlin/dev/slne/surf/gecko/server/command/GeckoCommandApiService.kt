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

/**
 * Installs the Minestom platform the command API compiles onto.
 *
 * This has to run before any command is registered - the API rejects a registration made while no
 * platform is installed - so it sits ahead of [GeckoCommandService] in the lifecycle. The port
 * itself carries no DI or event-registrar interface, hence this thin wrapper.
 */
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
