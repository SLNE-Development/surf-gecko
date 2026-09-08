package dev.slne.surf.gecko.server.gecko.lobby.view

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIcon
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.layoutTarget
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.minestom.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.minestom.inventory.framework.view.settings.builder.paginatedViewSettings
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

val geckoGamesView = paginatedSurfView("Spieleübersicht") {
    paginatedViewSettings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    settings {
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

        elementFactory { _, builder, _, game ->
            builder.renderWith {
                itemForGame(game)
            }
            builder.onClick { click ->
                click.closeForPlayer()

                if(GeckoGameManager.joinGame(click.player, game) == null) {
                    click.player.sendText {
                        appendPrefix()
                        geckoPrimary("Das Spiel ist nicht mehr verfügbar.")
                    }
                }
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

private fun itemForGame(geckoGame: GeckoGame) = ItemStack.builder(stateMaterial(geckoGame))
    .set(DataComponents.ITEM_NAME, buildText {
        geckoPrimary("Hide 'n Seek")
        appendSpace()
        geckoSecondary("(GeckoGames #${geckoGame.internalId})")
    })
    .set(DataComponents.LORE, listOf(Component.empty(), buildText {
        append(BitmapProvider.translateToComponent("Map", Colors.WHITE, Colors.INFO))
        decoration(TextDecoration.ITALIC, false)
    }, buildText {
        geckoSecondary(geckoGame.settings.map.mapDisplayName)
        decoration(TextDecoration.ITALIC, false)
    }, Component.empty(), buildText {
        append(BitmapProvider.translateToComponent("Spieler", Colors.WHITE, Colors.INFO))
        decoration(TextDecoration.ITALIC, false)
    }, buildText {
        geckoSecondary("${geckoGame.players.size} / ${geckoGame.settings.maxPlayers}")
        decoration(TextDecoration.ITALIC, false)
    }, Component.empty(), buildText {
        append(BitmapProvider.translateToComponent("Status", Colors.WHITE, Colors.INFO))
        decoration(TextDecoration.ITALIC, false)
    }, buildText {
        geckoSecondary(stateText(geckoGame))
        decoration(TextDecoration.ITALIC, false)
    }))
    .build()

private fun stateMaterial(geckoGame: GeckoGame) = if (geckoGame.joinable) {
    Material.LIME_STAINED_GLASS_PANE
} else {
    Material.RED_STAINED_GLASS_PANE
}

private fun stateText(geckoGame: GeckoGame) = when (geckoGame.state) {
    GeckoGameState.OFFLINE -> "Runde nicht verfügbar"
    GeckoGameState.LOBBY -> "Warten auf weitere Spieler..."
    GeckoGameState.HIDING -> "Spieler verstecken sich..."
    GeckoGameState.SEARCHING -> "Sucher suchen die Versteckten..."
    GeckoGameState.ENDING -> "Runde wird beendet..."
    GeckoGameState.ENDED -> "Runde ist zu Ende"
}