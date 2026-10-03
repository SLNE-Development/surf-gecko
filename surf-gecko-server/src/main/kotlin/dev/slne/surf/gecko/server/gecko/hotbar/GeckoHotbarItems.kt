package dev.slne.surf.gecko.server.gecko.hotbar

import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.icon.viewIcon
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

object GeckoHotbarItems {
    private fun lobbyItem(player: Player) = viewIcon(ViewIconType.HOME, ViewIconColor.RED) {
        displayName(player.translate("game.hotbar.lobby"))

        builder.withTag(GeckoHotbarAction.TAG, GeckoHotbarAction.LOBBY.name)
        builder.withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
    }

    private fun nextRoundItem(player: Player) = viewIcon(ViewIconType.RELOAD, ViewIconColor.GREEN) {
        displayName(player.translate("game.hotbar.next-round"))

        builder.withTag(GeckoHotbarAction.TAG, GeckoHotbarAction.NEXT_ROUND.name)
        builder.withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
    }

    private fun item(player: Player, action: GeckoHotbarAction): ItemStack = when (action) {
        GeckoHotbarAction.LOBBY -> lobbyItem(player)
        GeckoHotbarAction.NEXT_ROUND -> nextRoundItem(player)
    }

    fun giveGameLobbyItems(player: Player) {
        player.inventory.setItemStack(8, lobbyItem(player))
    }

    fun giveEndingItems(player: Player) {
        player.inventory.setItemStack(4, nextRoundItem(player))
        player.inventory.setItemStack(8, lobbyItem(player))
    }

    fun relocalize(player: Player) {
        val inventory = player.inventory

        for (slot in 0 until inventory.size) {
            val action = GeckoHotbarAction.of(inventory.getItemStack(slot)) ?: continue
            inventory.setItemStack(slot, item(player, action))
        }
    }
}
