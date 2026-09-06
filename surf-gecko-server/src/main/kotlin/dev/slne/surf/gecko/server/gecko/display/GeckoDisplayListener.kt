package dev.slne.surf.gecko.server.gecko.display

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import jakarta.inject.Singleton
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

@Singleton
class GeckoDisplayListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerLoginEvent::class.java, ::handleLogin)
    }

    private fun handleLogin(event: PlayerLoginEvent) {
        GeckoDisplayManager.showBossBar(event.player)
    }
}