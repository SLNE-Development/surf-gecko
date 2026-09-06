package dev.slne.surf.gecko.server.gecko.display

import dev.slne.minestom.lobby.api.event.EventRegistrar
import jakarta.inject.Singleton
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent

@Singleton
class GeckoDisplayListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerSpawnEvent::class.java, ::handleSpawn)
    }

    private fun handleSpawn(event: PlayerSpawnEvent) {
        if (!event.isFirstSpawn) {
            return
        }

        GeckoDisplayManager.showBossBar(event.player)
    }
}