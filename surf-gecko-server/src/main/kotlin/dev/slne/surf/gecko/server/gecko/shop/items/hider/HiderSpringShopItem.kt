package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect

object HiderSpringShopItem : ShopItem {
    override val id = "hider_spring"
    override val price = 3
    override val displayName = "Sprungfeder"
    override val description = "Katapultiert dich nach vorne und lässt dich sanft landen"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    override val item: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/spring").build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        player.velocity = player.position.direction().withY(0.0).normalize().mul(30.0).withY(20.0)
        player.addEffect(Potion(PotionEffect.SLOW_FALLING, 0, 3 * 20))
        player.playSound(GeckoSounds.SHOP_SPRING, Sound.Emitter.self())

        return true
    }
}
