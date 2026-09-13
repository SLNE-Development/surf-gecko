package dev.slne.surf.gecko.server.gecko.shop.items.misc

import dev.slne.surf.api.core.util.random
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import java.time.Duration

private const val STRIKE_CHANCE_PERCENT = 30
private const val STRIKE_DAMAGE = 5f
private val STRIKE_LIFETIME: Duration = Duration.ofMillis(500)

object MiscLightningShopItem : ShopItem {
    override val id = "misc_lightning"
    override val price = 100
    override val displayName = "Blitz"
    override val description = "Rufe einen Blitz herbei!"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER, GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/lightning").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame()) {
            return false
        }

        val targets = when (gamePlayer.role) {
            GeckoGameRole.HIDER -> game.activeSeekers()
            GeckoGameRole.SEEKER -> game.activeHiders()
            GeckoGameRole.SPECTATOR -> emptyList()
        }

        if (targets.isEmpty()) {
            player.sendShopItemMessage("Es gibt keine Spieler, die du mit dem Blitz treffen könntest!")
            return false
        }

        val struck = targets.filter { random.nextInt(100) < STRIKE_CHANCE_PERCENT }

        if (struck.isEmpty()) {
            player.sendShopItemMessage("Der Blitz hat niemanden getroffen.")
            return true
        }

        struck.forEach { strike(it, player) }

        player.sendShopItemMessage("Der Blitz hat ${struck.size} Spieler getroffen.")

        return true
    }

    private fun strike(target: Player, caster: Player) {
        val instance = target.instance ?: return
        val bolt = Entity(EntityType.LIGHTNING_BOLT)

        bolt.setInstance(instance, target.position)
        bolt.scheduleRemove(STRIKE_LIFETIME)

        target.damage(
            Damage(
                DamageType.LIGHTNING_BOLT,
                null,
                caster,
                target.position,
                STRIKE_DAMAGE
            )
        )
    }
}
