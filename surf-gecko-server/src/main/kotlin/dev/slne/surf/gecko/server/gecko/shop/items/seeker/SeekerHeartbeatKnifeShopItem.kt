package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.heart.HeartbeatEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

object SeekerHeartbeatKnifeShopItem : ShopItem {
    override val id = "seeker_heartbeat_knife"
    override val price = 6
    override val displayName = "Herzschlag-Messer"
    override val description = "Höre den Herzschlag der Verstecker in deiner Nähe"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.RED_DYE)

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!playOnce(player, 15.seconds, HeartbeatEffect(player))) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Dein Herzschlag-Messer ist bereits aktiv.")
            }
            return false
        }

        return true
    }
}
