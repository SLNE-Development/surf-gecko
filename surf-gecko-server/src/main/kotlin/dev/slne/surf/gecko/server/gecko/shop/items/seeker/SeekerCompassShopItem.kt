package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.effect.compass.CompassEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.shop.nearestTo
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.sendTranslated
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

object SeekerCompassShopItem : ShopItem {
    override val id = "seeker_compass"
    override val price = 6
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_ATTACK)

    override val item: ItemStack = ItemStack.of(Material.COMPASS)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (game.activeHiders().nearestTo(player) == null) {
            return true
        }

        val effect = CompassEffect(player, displayName) { game.activeHiders().nearestTo(player) }

        if (!playOnce(player, 10.seconds, effect)) {
            player.sendTranslated("shop.item.seeker_compass.already-active")
            return false
        }

        player.playSound(GeckoSounds.SHOP_SONAR_PING, Sound.Emitter.self())

        return true
    }
}
