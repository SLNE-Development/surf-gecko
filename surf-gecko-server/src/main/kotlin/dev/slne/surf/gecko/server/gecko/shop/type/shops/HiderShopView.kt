package dev.slne.surf.gecko.server.gecko.shop.type.shops

import dev.slne.surf.api.minestom.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIcon
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.onInit
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.state.get
import dev.slne.surf.api.minestom.inventory.framework.view.state.initialState
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.ShopPurchase
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import net.kyori.adventure.text.Component
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.random.Random

val hiderShopView = surfView("Verstecker Shop") {
    val mapState = initialState<GeckoMap>("map")
    val seedState = initialState<Long>("seed")

    settings {
        rows(4)
        cancelAllInteractions()
        navigateBackOnOutsideClick(false)
    }

    onInit {
        layout("010203040", " A D T P ", " A D T P ", "X00000000")
    }

    onFirstRender {
        val map = mapState[this]
        val random = Random(seedState[this])
        val attackItems =
            ShopItem.byType(ShopItemType.HIDER_ATTACK).filter { it.availableOn(map) }
                .shuffled(random)
        val defenseItems =
            ShopItem.byType(ShopItemType.HIDER_DEFENSE).filter { it.availableOn(map) }
                .shuffled(random)
        val trollItems =
            ShopItem.byType(ShopItemType.HIDER_TROLL).filter { it.availableOn(map) }
                .shuffled(random)
        val potionItems =
            ShopItem.byType(ShopItemType.HIDER_POTION).filter { it.availableOn(map) }
                .shuffled(random)

        layoutSlot(
            '0', ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
                .set(DataComponents.ITEM_NAME, Component.empty()).build()
        )

        layoutSlot('1', ShopItemType.SEEKER_ATTACK.item)
        layoutSlot('2', ShopItemType.HIDER_DEFENSE.item)
        layoutSlot('3', ShopItemType.HIDER_TROLL.item)
        layoutSlot('4', ShopItemType.HIDER_POTION.item)

        layoutSlot('X', ViewIcon(ViewIconType.CROSS, ViewIconColor.RED).build {
            displayName {
                error("Schließen")
            }
        }).onClick { click ->
            click.closeForPlayer()
        }

        layoutSlot('A') { index, builder ->
            val item = attackItems.getOrNull(index)

            if (item != null) {
                builder.onItemClick { ShopPurchase.buy(player, item) }
            }

            builder.withItem(
                item?.let {
                    buildShopItemDisplay(it)
                } ?: ItemStack.AIR
            )
        }

        layoutSlot('D') { index, builder ->
            val item = defenseItems.getOrNull(index)

            if (item != null) {
                builder.onItemClick { ShopPurchase.buy(player, item) }
            }

            builder.withItem(
                item?.let {
                    buildShopItemDisplay(it)
                } ?: ItemStack.AIR
            )
        }

        layoutSlot('T') { index, builder ->
            val item = trollItems.getOrNull(index)

            if (item != null) {
                builder.onItemClick { ShopPurchase.buy(player, item) }
            }

            builder.withItem(
                item?.let {
                    buildShopItemDisplay(it)
                } ?: ItemStack.AIR
            )
        }

        layoutSlot('P') { index, builder ->
            val item = potionItems.getOrNull(index)

            if (item != null) {
                builder.onItemClick { ShopPurchase.buy(player, item) }
            }

            builder.withItem(
                item?.let {
                    buildShopItemDisplay(it)
                } ?: ItemStack.AIR
            )
        }
    }
}