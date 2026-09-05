package dev.slne.surf.gecko.server.gecko.player.listener

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import jakarta.inject.Singleton
import kotlinx.coroutines.launch
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

@Singleton
class GeckoPlayerListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockPlaceEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockBreakEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockInteractEvent::class.java) { cancel(it, it.player) }
        node.addListener(ItemDropEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerUseItemEvent::class.java) { event ->
            if (event.itemStack.material() in USABLE_MATERIALS) {
                return@addListener
            }

            cancel(event, event.player)
        }
        node.addListener(PlayerDisconnectEvent::class.java) {
            geckoAsyncScope.launch {
                GeckoGameManager.handleGameLeave(it.player)
            }
        }
        node.addListener(InventoryPreClickEvent::class.java) {
            if (it.clickedItem.hasTag(GECKO_ITEM_TAG)) {
                it.isCancelled = true
            }
        }

        node.addListener(PlayerSwapItemEvent::class.java) {
            if (it.offHandItem.hasTag(GECKO_ITEM_TAG)) {
                it.isCancelled = true
            }
        }
    }

    private fun cancel(event: CancellableEvent, player: Player) {
        if (hasBypass(player)) {
            return
        }

        event.isCancelled = true
    }

    private fun hasBypass(player: Player) = player.gameMode == GameMode.CREATIVE

    companion object {
        val USABLE_MATERIALS = setOf(Material.BOW, Material.CHEST)
        val GECKO_ITEM_TAG: Tag<Boolean> = Tag.Boolean("gecko_item")
    }
}
