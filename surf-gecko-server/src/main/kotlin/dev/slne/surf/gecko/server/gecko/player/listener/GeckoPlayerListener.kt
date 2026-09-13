package dev.slne.surf.gecko.server.gecko.player.listener

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbs
import jakarta.inject.Singleton
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
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
        node.addListener(InventoryPreClickEvent::class.java) { handleInventoryClick(it) }

        node.addListener(PlayerSwapItemEvent::class.java) {
            if (it.offHandItem.hasTag(GECKO_ITEM_TAG)) {
                it.isCancelled = true
            }
        }
    }

    private fun handleInventoryClick(event: InventoryPreClickEvent) {
        val player = event.player
        val clicked = event.clickedItem
        val cursor = player.inventory.cursorItem

        if (!clicked.hasTag(GECKO_ITEM_TAG) && !cursor.hasTag(GECKO_ITEM_TAG)) {
            return
        }

        if (!isOrbOnly(clicked) || !isOrbOnly(cursor)) {
            event.isCancelled = true
            return
        }

        if (player.openInventory != null || !isMove(event.click)) {
            event.isCancelled = true
        }
    }

    private fun isOrbOnly(item: ItemStack) =
        item.isAir || item.hasTag(GeckoOrbs.ORB_TAG_KEY)

    private fun isMove(click: Click) = when (click) {
        is Click.Left,
        is Click.Right,
        is Click.Double,
        is Click.LeftShift,
        is Click.RightShift,
        is Click.HotbarSwap,
        is Click.Drag -> true

        else -> false
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
