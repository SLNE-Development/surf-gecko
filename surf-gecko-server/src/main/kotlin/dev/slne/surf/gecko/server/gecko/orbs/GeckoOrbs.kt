package dev.slne.surf.gecko.server.gecko.orbs

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

object GeckoOrbs {
    val ORB_TAG_KEY: Tag<Boolean> = Tag.Boolean("gecko_orb")

    val ITEM: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "nexo:gecko/orb")
        .set(DataComponents.ITEM_NAME, buildText { geckoHighlight("Orb") })
        .withTag(ORB_TAG_KEY, true)
        .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
        .build()

    fun count(player: Player) = (0 until PlayerInventory.INNER_INVENTORY_SIZE)
        .map { player.inventory.getItemStack(it) }
        .filter { it.hasTag(ORB_TAG_KEY) }
        .sumOf { it.amount() }

    fun give(player: Player, amount: Int) {
        if (amount <= 0) {
            return
        }

        var remaining = amount

        for (slot in 0 until PlayerInventory.INNER_INVENTORY_SIZE) {
            if (remaining <= 0) {
                break
            }

            val stack = player.inventory.getItemStack(slot)

            if (!stack.hasTag(ORB_TAG_KEY)) {
                continue
            }

            val space = stack.maxStackSize() - stack.amount()
            val toGive = minOf(remaining, space)

            remaining -= toGive
            player.inventory.setItemStack(slot, stack.withAmount(toGive))
        }

        while (remaining > 0) {
            val toGive = minOf(remaining, ITEM.maxStackSize())
            remaining -= toGive

            player.inventory.addItemStack(ITEM.withAmount(toGive))
        }
    }

    fun take(player: Player, amount: Int): Boolean {
        if (amount <= 0) {
            return true
        }

        if (count(player) < amount) {
            return false
        }

        var remaining = amount

        for (slot in 0 until PlayerInventory.INNER_INVENTORY_SIZE) {
            if (remaining <= 0) {
                break
            }

            val stack = player.inventory.getItemStack(slot)

            if (!stack.hasTag(ORB_TAG_KEY)) {
                continue
            }

            val taken = minOf(remaining, stack.amount())

            remaining -= taken
            player.inventory.setItemStack(slot, stack.consume(taken))
        }

        return true
    }
}
