package dev.slne.surf.gecko.server.gecko.player.listener

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.geckoLogger
import jakarta.inject.Singleton
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerUseItemEvent

@Singleton
class GeckoPlayerListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockPlaceEvent::class.java, ::handleBlockPlace)
        node.addListener(PlayerBlockPlaceEvent::class.java, ::handleBlockBreak)
        node.addListener(PlayerBlockInteractEvent::class.java, ::handleInteraction)
        node.addListener(ItemDropEvent::class.java, ::handleDrop)
        node.addListener(PlayerUseItemEvent::class.java, ::handleUse)
    }

    private fun handleBlockPlace(event: PlayerBlockPlaceEvent) {
        geckoLogger.info("aaaaa")
        if (!hasBypass(event.player)) {
            return
        }

        event.isCancelled = true
    }

    private fun handleBlockBreak(event: PlayerBlockPlaceEvent) {
        if (!hasBypass(event.player)) {
            return
        }

        event.isCancelled = true
    }

    private fun handleInteraction(event: PlayerBlockInteractEvent) {
        if (!hasBypass(event.player)) {
            return
        }

        event.isCancelled = true
    }

    private fun handleDrop(event: ItemDropEvent) {
        if (!hasBypass(event.player)) {
            return
        }

        event.isCancelled = true
    }

    private fun handleUse(event: PlayerUseItemEvent) {
        if (!hasBypass(event.player)) {
            return
        }

        event.isCancelled = true
    }


    private fun hasBypass(player: Player) = player.gameMode == GameMode.CREATIVE
}