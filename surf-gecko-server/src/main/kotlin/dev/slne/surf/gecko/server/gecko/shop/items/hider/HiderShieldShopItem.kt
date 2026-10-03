package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.sendTranslated
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect

object HiderShieldShopItem : ShopItem {
    override val id = "hider_shield"
    override val price = 5
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)
    override val types = listOf(ShopItemType.HIDER_DEFENSE)

    override val item: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/shield").build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        val durationTicks = 10 * 20

        player.addEffect(Potion(PotionEffect.RESISTANCE, 1, durationTicks))
        player.addEffect(Potion(PotionEffect.ABSORPTION, 1, durationTicks))

        player.playSound(GeckoSounds.SHOP_SHIELD, Sound.Emitter.self())
        player.sendTranslated("shop.item.hider_shield.activated")

        return true
    }
}
