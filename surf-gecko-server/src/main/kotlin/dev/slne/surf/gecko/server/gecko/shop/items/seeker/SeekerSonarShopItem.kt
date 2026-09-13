package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.shop.within
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

private const val RADIUS = 40.0

object SeekerSonarShopItem : ShopItem {
    override val id = "seeker_sonar"
    override val price = 4
    override val displayName = "Sonar"
    override val description = "Sendet einen Ping, der jeden Verstecker in 40 Blöcken hörbar macht"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/sonar").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        val hiders = game.activeHiders().within(player, RADIUS)

        if (hiders.isEmpty()) {
            player.sendShopItemMessage("Das Sonar findet keinen Verstecker in Reichweite.")
            return false
        }

        hiders.forEach {
            player.playSound(
                GeckoSounds.SHOP_SONAR_PING,
                it.position.x,
                it.position.y,
                it.position.z
            )
        }

        player.sendActionBar(buildText {
            geckoHighlight(hiders.size.toString())
            geckoPrimary(" Verstecker in Reichweite")
        })

        return true
    }
}
