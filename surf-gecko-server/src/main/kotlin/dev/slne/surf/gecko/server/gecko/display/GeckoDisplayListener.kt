package dev.slne.surf.gecko.server.gecko.display

import dev.slne.surf.api.minestom.event.EventRegistrar
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent

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