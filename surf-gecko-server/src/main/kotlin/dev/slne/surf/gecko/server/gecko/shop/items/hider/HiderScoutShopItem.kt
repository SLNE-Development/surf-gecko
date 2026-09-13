package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoGlowEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopItemUsages
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

private const val DURATION_MILLIS = 10_000L

object HiderScoutShopItem : ShopItem {
    override val id = "hider_scout"
    override val price = 6
    override val displayName = "Ausguck"
    override val description = "Zeigt dir 10 Sekunden lang alle Sucher durch Wände"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/scout").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        val seekers = game.activeSeekers()

        if (seekers.isEmpty()) {
            player.sendShopItemMessage("Der Ausguck findet keinen Sucher.")
            return false
        }

        if (!ShopItemUsages.start(id, player)) {
            player.sendShopItemMessage("Dein Ausguck ist bereits aktiv.")
            return false
        }

        player.playSound(GeckoSounds.SHOP_SPY_DEVICE, Sound.Emitter.self())
        player.sendShopItemMessage("Der Ausguck zeigt dir 10 Sekunden lang alle Sucher.")

        geckoScope.launch {
            try {
                val until = System.currentTimeMillis() + DURATION_MILLIS

                while (System.currentTimeMillis() < until && player.isOnline) {
                    seekers.forEach { GeckoGlowEffect.send(player, it, true) }
                    delay(1.seconds)
                }
            } finally {
                if (player.isOnline) {
                    seekers.filter { it.isOnline }
                        .forEach { GeckoGlowEffect.send(player, it, false) }
                }

                ShopItemUsages.finish(id, player)
            }
        }

        return true
    }
}
