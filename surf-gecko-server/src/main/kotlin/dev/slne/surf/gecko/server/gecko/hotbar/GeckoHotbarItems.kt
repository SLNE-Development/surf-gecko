package dev.slne.surf.gecko.server.gecko.hotbar

import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.icon.viewIcon
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.entity.Player

object GeckoHotbarItems {
    private val lobbyItem = viewIcon(ViewIconType.HOME, ViewIconColor.RED) {
        displayName {
            geckoPrimary("Zurück zur Lobby")
        }

        builder.withTag(GeckoHotbarAction.TAG, GeckoHotbarAction.LOBBY.name)
        builder.withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
    }

    private val nextRoundItem = viewIcon(ViewIconType.RELOAD, ViewIconColor.RED) {
        displayName {
            geckoPrimary("Nächste Runde")
        }

        builder.withTag(GeckoHotbarAction.TAG, GeckoHotbarAction.NEXT_ROUND.name)
        builder.withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
    }

    fun giveGameLobbyItems(player: Player) {
        player.inventory.setItemStack(8, lobbyItem)
    }

    fun giveEndingItems(player: Player) {
        player.inventory.setItemStack(4, nextRoundItem)
        player.inventory.setItemStack(8, lobbyItem)
    }
}
