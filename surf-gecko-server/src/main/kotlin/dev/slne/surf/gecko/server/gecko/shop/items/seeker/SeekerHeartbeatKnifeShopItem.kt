package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.heart.HeartbeatEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.i18n.sendTranslated
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

object SeekerHeartbeatKnifeShopItem : ShopItem {
    override val id = "seeker_heartbeat_knife"
    override val price = 6
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_SEARCH)

    override val item: ItemStack = ItemStack.of(Material.RED_DYE)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!playOnce(player, 15.seconds, HeartbeatEffect(player))) {
            player.sendTranslated("shop.item.seeker_heartbeat_knife.already-active")
            return false
        }

        return true
    }
}
