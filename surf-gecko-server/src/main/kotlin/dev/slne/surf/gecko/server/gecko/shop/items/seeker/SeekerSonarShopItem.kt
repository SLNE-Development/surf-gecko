package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.shop.within
import dev.slne.surf.gecko.server.gecko.visual.SonarWave
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object SeekerSonarShopItem : ShopItem {
    override val id = "seeker_sonar"
    override val price = 7
    override val displayName = "Sonar"
    override val description = "Zeigt dir alle Verstecker im Umkreis von 15 Blöcken"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_SEARCH)

    override val item: ItemStack = ItemStack.of(Material.ECHO_SHARD)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        SonarWave.scan(player, 15.0, game.activeHiders().within(player, 15.0))

        return true
    }
}
