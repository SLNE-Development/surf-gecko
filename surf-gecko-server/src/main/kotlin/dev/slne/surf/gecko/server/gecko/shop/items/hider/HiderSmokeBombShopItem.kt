package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopProjectiles
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.shop.within
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect

private const val RADIUS = 8.0
private const val DURATION_TICKS = 5 * 20
private const val THROW_POWER = 1.4
private const val PARTICLE_COUNT = 80
private val PARTICLE_SPREAD = Vec(1.8, 1.2, 1.8)

object HiderSmokeBombShopItem : ShopItem {
    const val PROJECTILE_KIND = "smoke_bomb"

    override val id = "hider_smoke_bomb"
    override val price = 7
    override val displayName = "Rauchbombe"
    override val description = "Wirf eine Bombe, die alle Sucher im Umkreis blendet"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "surf:gecko/shop/smoke_bomb").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        if (!ShopProjectiles.launch(player, PROJECTILE_KIND, EntityType.SNOWBALL, THROW_POWER)) {
            return false
        }

        player.playSound(GeckoSounds.SHOP_SMOKE_BOMB, Sound.Emitter.self())

        return true
    }

    fun detonate(instance: Instance, position: Pos, thrower: Player?) {
        instance.sendGroupedPacket(
            ParticlePacket(Particle.LARGE_SMOKE, position, PARTICLE_SPREAD, 0.02f, PARTICLE_COUNT)
        )
        instance.playSound(GeckoSounds.SHOP_SMOKE_BOMB, position.x, position.y, position.z)

        val game = thrower?.let { GeckoGameManager.findGame(it.uuid) } ?: return
        val blinded = game.activeSeekers().within(instance, position, RADIUS)

        if (blinded.isEmpty()) {
            thrower.sendShopItemMessage("Die Rauchbombe hat keinen Sucher erwischt.")
            return
        }

        blinded.forEach {
            it.addEffect(Potion(PotionEffect.BLINDNESS, 0, DURATION_TICKS))
            it.addEffect(Potion(PotionEffect.SLOWNESS, 0, DURATION_TICKS))
            it.sendShopItemMessage("Eine Rauchbombe hat dich geblendet!")
        }

        thrower.sendShopItemMessage("Die Rauchbombe hat ${blinded.size} Sucher geblendet.")
    }
}
