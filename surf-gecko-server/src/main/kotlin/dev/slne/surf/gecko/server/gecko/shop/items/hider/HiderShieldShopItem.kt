package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect

private const val DURATION_TICKS = 10 * 20

object HiderShieldShopItem : ShopItem {
    override val id = "hider_shield"
    override val price = 5
    override val displayName = "Schutzschild"
    override val description = "Schützt dich 10 Sekunden lang vor Schaden"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/shield").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        player.addEffect(Potion(PotionEffect.RESISTANCE, 1, DURATION_TICKS))
        player.addEffect(Potion(PotionEffect.ABSORPTION, 1, DURATION_TICKS))
        player.playSound(GeckoSounds.SHOP_SHIELD, Sound.Emitter.self())
        player.sendShopItemMessage("Dein Schutzschild hält 10 Sekunden.")

        return true
    }
}
