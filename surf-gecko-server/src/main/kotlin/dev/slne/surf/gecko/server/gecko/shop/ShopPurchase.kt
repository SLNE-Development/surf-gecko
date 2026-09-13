package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbs
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
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
            player.sendText {
                appendPrefix()
                geckoPrimary("Du hast nicht genug Orbs.")
            }
            return
        }

        if (!player.inventory.addItemStack(item.inventoryItem)) {
            player.playSound(GeckoSounds.SHOP_DENY, Sound.Emitter.self())
            player.sendText {
                appendPrefix()
                geckoPrimary("Du hast keinen Platz mehr im Inventar.")
            }
            return
        }

        GeckoOrbs.take(player, item.price)
        player.playSound(GeckoSounds.SHOP_BUY, Sound.Emitter.self())
        player.sendText {
            appendPrefix()
            geckoPrimary("Du hast ")
            geckoSecondary(item.displayName)
            geckoPrimary(" gekauft.")
        }
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
