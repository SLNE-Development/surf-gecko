package dev.slne.surf.gecko.server.gecko.lobby.listener

import dev.slne.minestom.lobby.api.event.EventRegistrar
import jakarta.inject.Singleton
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.event.trait.CancellableEvent

@Singleton
class GeckoLobbyListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockPlaceEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockBreakEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockInteractEvent::class.java) { cancel(it, it.player) }
        node.addListener(ItemDropEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerUseItemEvent::class.java) { cancel(it, it.player) }
    }

    private fun cancel(event: CancellableEvent, player: Player) {
        if (hasBypass(player)) {
            return
        }

        event.isCancelled = true
    }

    private fun hasBypass(player: Player) = player.gameMode == GameMode.CREATIVE
}