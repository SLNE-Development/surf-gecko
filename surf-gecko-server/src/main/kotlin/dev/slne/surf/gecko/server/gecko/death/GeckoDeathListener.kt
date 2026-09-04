package dev.slne.surf.gecko.server.gecko.death

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDeathEvent

@Singleton
class GeckoDeathListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerDeathEvent::class.java) { handleDeath(it) }
    }

    private fun handleDeath(event: PlayerDeathEvent) {

    }
}