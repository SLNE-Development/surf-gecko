package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoGlowEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopItemUsages
import dev.slne.surf.gecko.server.gecko.shop.nearestTo
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

private const val DURATION_MILLIS = 30_000L

object SeekerSpyDeviceShopItem : ShopItem {
    override val id = "seeker_spy_device"
    override val price = 8
    override val displayName = "Spionagegerät"
    override val description = "Markiert den nähesten Verstecker 30 Sekunden lang nur für dich"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/spy_device").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        val target = game.activeHiders().nearestTo(player)

        if (target == null) {
            player.sendShopItemMessage("Das Spionagegerät findet keinen Verstecker.")
            return false
        }

        if (!ShopItemUsages.start(id, player)) {
            player.sendShopItemMessage("Dein Spionagegerät ist bereits aktiv.")
            return false
        }

        player.sendShopItemMessage("Das Spionagegerät hat einen Verstecker markiert.")
        player.playSound(GeckoSounds.SHOP_SPY_DEVICE, Sound.Emitter.self())

        geckoScope.launch {
            try {
                val until = System.currentTimeMillis() + DURATION_MILLIS

                while (System.currentTimeMillis() < until && player.isOnline && target.isOnline) {
                    GeckoGlowEffect.send(player, target, true)
                    delay(1.seconds)
                }
            } finally {
                if (player.isOnline && target.isOnline) {
                    GeckoGlowEffect.send(player, target, false)
                }

                ShopItemUsages.finish(id, player)
            }
        }

        return true
    }
}
