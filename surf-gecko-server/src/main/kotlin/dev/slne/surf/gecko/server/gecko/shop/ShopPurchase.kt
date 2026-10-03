package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbs
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.language
import dev.slne.surf.gecko.server.i18n.sendTranslated
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.inventory.PlayerInventory

object ShopPurchase {
    fun buy(player: Player, item: ShopItem) {
        val game = GeckoGameManager.findGame(player.uuid) ?: return
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return

        if (!game.state.isGame() ||
            !item.roles.contains(gamePlayer.role) ||
            !item.availableOn(game.settings.map)
        ) {
            return
        }

        val balance = GeckoOrbs.count(player)

        if (balance < item.price) {
            player.playSound(GeckoSounds.SHOP_DENY, Sound.Emitter.self())
            player.sendTranslated("shop.purchase.not-enough-orbs")
            return
        }

        if (!player.inventory.addItemStack(item.inventoryItem(player.language))) {
            player.playSound(GeckoSounds.SHOP_DENY, Sound.Emitter.self())
            player.sendTranslated("shop.purchase.inventory-full")
            return
        }

        GeckoOrbs.take(player, item.price)
        player.playSound(GeckoSounds.SHOP_BUY, Sound.Emitter.self())
        player.sendTranslated("shop.purchase.success", "item" to item.displayName)
    }

    fun consume(player: Player, item: ShopItem) {
        for (slot in 0 until PlayerInventory.INNER_INVENTORY_SIZE) {
            val stack = player.inventory.getItemStack(slot)

            if (stack.getTag(ShopItem.ID_TAG) != item.id) {
                continue
            }

            player.inventory.setItemStack(slot, stack.consume(1))
            return
        }
    }
}
