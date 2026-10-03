package dev.slne.surf.gecko.server.gecko.lobby.view

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.minestom.inventory.framework.modifyConfig
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIcon
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.layoutTarget
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.onOpen
import dev.slne.surf.api.minestom.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.minestom.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.gecko.server.i18n.translatePlain
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

val geckoGamesView = paginatedSurfView("Spieleübersicht") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    onOpen {
        val title = player.translate("lobby.games.title")
        modifyConfig { title(title) }
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

        elementFactory { context, builder, _, game ->
            builder.renderWith {
                itemForGame(context.player, game)
            }
            builder.onClick { click ->
                click.closeForPlayer()

                if (GeckoGameManager.joinGame(click.player, game) == null) {
                    click.player.sendTranslated("lobby.games.unavailable")
                }
            }
        }
    }

    layoutTarget('I')

    onFirstRender {
        slot(4, 9, newGameItem(player))
        slot(4, 1, ViewIcon(ViewIconType.CROSS, ViewIconColor.RED).build {
            displayName(player.translate("common.menu.close"))
        }).onClick { click ->
            click.closeForPlayer()
        }
    }
}

private fun newGameItem(player: Player) = ViewIcon(ViewIconType.PLUS, ViewIconColor.GREEN).build {
    displayName(player.translate("lobby.games.create"))
}

private fun itemForGame(player: Player, geckoGame: GeckoGame) =
    ViewIcon(ViewIconType.HOME, stateColor(geckoGame)).build {
        displayName(player.translate("lobby.games.item.name", "id" to geckoGame.internalId))
        lore(
            Component.empty(),
            heading(player, "lobby.games.item.heading.map"),
            player.translate("lobby.games.item.map", "map" to geckoGame.settings.map.displayName),
            Component.empty(),
            heading(player, "lobby.games.item.heading.players"),
            player.translate(
                "lobby.games.item.players",
                "players" to geckoGame.players.size,
                "max" to geckoGame.settings.maxPlayers
            ),
            Component.empty(),
            heading(player, "lobby.games.item.heading.status"),
            player.translate(stateKey(geckoGame)),
        )
    }

private fun heading(player: Player, key: String) =
    BitmapProvider.translateToComponent(player.translatePlain(key), Colors.WHITE, Colors.INFO)

private fun stateColor(geckoGame: GeckoGame) = if (geckoGame.joinable) {
    ViewIconColor.GREEN
} else {
    ViewIconColor.RED
}

private fun stateKey(geckoGame: GeckoGame) = when (geckoGame.state) {
    GeckoGameState.OFFLINE -> "lobby.games.state.offline"
    GeckoGameState.LOBBY -> "lobby.games.state.lobby"
    GeckoGameState.HIDING -> "lobby.games.state.hiding"
    GeckoGameState.SEARCHING -> "lobby.games.state.searching"
    GeckoGameState.ENDING -> "lobby.games.state.ending"
    GeckoGameState.ENDED -> "lobby.games.state.ended"
}
