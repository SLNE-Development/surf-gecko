package dev.slne.surf.gecko.server.gecko.shop.items.misc

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.util.random
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object MiscLightningShopItem : ShopItem {
    override val id = "misc_lightning"
    override val price = 100
    override val displayName = "Blitz"
    override val description = "Rufe einen Blitz herbei!"
    override val displayItem: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/lightning").build()
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER, GeckoGameRole.SEEKER)
    override val inventoryItem: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/lightning").build()

    override fun onUse(player: Player) {
        val game = GeckoGameManager.findGame(player.uuid) ?: return
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return

        val targets = when (gamePlayer.role) {
            GeckoGameRole.HIDER -> game.gamePlayers.filter { it.role == GeckoGameRole.SEEKER }
            GeckoGameRole.SEEKER -> game.gamePlayers.filter { it.role == GeckoGameRole.HIDER }
            else -> null
        }

        if (targets.isNullOrEmpty()) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Es gibt keine Spieler, die du mit dem Blitz treffen könntest!")
            }
            return
        }

        targets.filter { random.nextInt(10) < 3 }.forEach {
            // TODO: Lightning effect
        }
    }
}