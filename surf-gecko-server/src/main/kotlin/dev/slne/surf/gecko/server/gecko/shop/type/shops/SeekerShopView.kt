package dev.slne.surf.gecko.server.gecko.shop.type.shops

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.onInit
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.state.get
import dev.slne.surf.api.minestom.inventory.framework.view.state.initialState
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.ShopPurchase
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack

val seekerShopView = surfView("Sucher Shop") {
    val mapState = initialState<GeckoMap>("map")

    settings {
        rows(4)
        cancelAllInteractions()
    }

    onInit {
        layout("OO")
    }

    onFirstRender {
        val map = mapState[this]
        val items = ShopItem.byRole(roleState[this]).filter { it.availableOn(map) }

        layoutSlot('I') { index, builder ->
            val item = items.getOrNull(index)

            if (item != null) {
                builder.onItemClick { ShopPurchase.buy(player, item) }
            }

            builder.withItem(
                item?.let {
                    it.inventoryItem.builder()
                        .set(DataComponents.ITEM_NAME, buildText { note(it.displayName) })
                        .set(
                            DataComponents.LORE,
                            mutableListOf<Component>(
                                buildText {
                                    spacer(it.description).decoration(
                                        TextDecoration.ITALIC,
                                        false
                                    )
                                },
                                Component.empty(),
                                buildText {
                                    variableValue(it.price)
                                    white(" Orbs")
                                }
                            )
                        )
                        .build()
                } ?: ItemStack.AIR
            )
        }
    }
}
}