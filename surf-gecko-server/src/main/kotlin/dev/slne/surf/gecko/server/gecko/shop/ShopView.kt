package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.onInit
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.state.get
import dev.slne.surf.api.minestom.inventory.framework.view.state.initialState
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack

val shopView by lazy {
    surfView("Shop") {
        val roleState = initialState<GeckoGameRole>("role")

        settings {
            rows(1)
            cancelAllInteractions()
        }

        onInit {
            layout("IIIIIIIII")
        }

        onFirstRender {
            val items = ShopItem.byRole(roleState[this])

            layoutSlot('I') { index, builder ->
                val item = items.getOrNull(index)

                builder.withItem(
                    item?.let {
                        ItemStack.builder(it.displayMaterial)
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
