package dev.slne.surf.gecko.server.gecko.shop.type.shops

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.gecko.server.gecko.util.removeItalics
import net.kyori.adventure.text.Component
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack

fun buildShopItemDisplay(shopItem: ShopItem): ItemStack = shopItem.inventoryItem.builder()
    .set(DataComponents.ITEM_NAME, buildText { note(shopItem.displayName) })
    .set(
        DataComponents.LORE,
        mutableListOf<Component>(
            buildText {
                spacer(shopItem.description)
            },
            Component.empty(),
            buildText {
                geckoPrimary("Preis: ")
                geckoHighlight(shopItem.price.toString())
                geckoSecondary(" Orbs")
            }
        ).removeItalics()
    )
    .build()