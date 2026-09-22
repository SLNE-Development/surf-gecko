package dev.slne.surf.gecko.server.gecko.shop.type

import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.gecko.server.gecko.util.GECKO_HIGHLIGHT
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

enum class ShopItemType(val item: ItemStack) {
    HIDER_ATTACK(
        ItemStack.builder(Material.IRON_SWORD)
            .set(DataComponents.ITEM_NAME, text("Angriff", GECKO_HIGHLIGHT))
            .build()
    ),
    HIDER_DEFENSE(
        ItemStack.builder(Material.SHIELD)
            .set(DataComponents.ITEM_NAME, text("Verteidigung", GECKO_HIGHLIGHT))
            .build()
    ),
    HIDER_TROLL(
        ItemStack.builder(Material.CARROT_ON_A_STICK)
            .set(DataComponents.ITEM_NAME, text("Weiteres", GECKO_HIGHLIGHT))
            .build()
    ),
    HIDER_POTION(
        ItemStack.builder(Material.POTION)
            .set(DataComponents.ITEM_NAME, text("Trank", GECKO_HIGHLIGHT))
            .build()
    ),

    SEEKER_ATTACK(
        ItemStack.builder(Material.IRON_SWORD)
            .set(DataComponents.ITEM_NAME, text("Angriff", GECKO_HIGHLIGHT))
            .build()
    ),
    SEEKER_SEARCH(
        ItemStack.builder(Material.BOW)
            .set(DataComponents.ITEM_NAME, text("Suche", GECKO_HIGHLIGHT))
            .build()
    ),
    SEEKER_POTION(
        ItemStack.builder(Material.POTION)
            .set(DataComponents.ITEM_NAME, text("Trank", GECKO_HIGHLIGHT))
            .build()
    ),
}