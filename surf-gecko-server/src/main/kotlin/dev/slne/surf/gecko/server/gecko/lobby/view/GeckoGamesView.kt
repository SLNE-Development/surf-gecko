package dev.slne.surf.gecko.server.gecko.lobby.view

import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIcon
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.layoutTarget
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.minestom.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.minestom.inventory.framework.view.settings.builder.paginatedViewSettings
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

val geckoGamesView = paginatedSurfView("Games") {
    paginatedViewSettings {
        paginationViewRows(PaginationViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    pagination {
        computedSource {
            GeckoGameManager.getGames()
                .sortedWith(
                    compareByDescending<GeckoGame> { it.joinable }
                        .thenByDescending { it.players.size }
                        .thenBy { it.settings.map.mapDisplayName }
                )
        }

        elementFactory { _, builder, _, value ->
            builder.renderWith {
                itemForGame(value)
            }
        }
    }

    layoutTarget('I')

    onFirstRender {
        slot(4, 9, newGameItem())
    }
}

private fun newGameItem() = ViewIcon(ViewIconType.PLUS, ViewIconColor.GREEN).build {
    displayName {
        success("Neue Runde erstellen")
    }
}

private

fun itemForGame(geckoGame: GeckoGame) = if (geckoGame.joinable) {
    ItemStack.builder(Material.LIME_STAINED_GLASS_PANE).build()
} else {
    ItemStack.builder(Material.RED_STAINED_GLASS_PANE).build()
}