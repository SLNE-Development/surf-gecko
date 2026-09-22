package dev.slne.surf.gecko.server.gecko.shop.items.misc

import dev.slne.surf.api.core.util.random
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object MiscLightningShopItem : ShopItem {
    override val id = "misc_lightning"
    override val price = 5
    override val displayName = "Blitz"
    override val description = "Lasse deine Gegner mit einen Blitz treffen!"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER, GeckoGameRole.SEEKER)

    override val item: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/lightning").build()

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

        targets.filter { random.nextInt(100) < 30 }.forEach { strike(it, player) }

        return true
    }

    private fun strike(target: Player, caster: Player) {
        val instance = target.instance ?: return
        val bolt = Entity(EntityType.LIGHTNING_BOLT)

        bolt.setInstance(instance, target.position)

        target.damage(
            Damage(
                DamageType.LIGHTNING_BOLT,
                null,
                caster,
                target.position,
                5f
            )
        )
    }
}
