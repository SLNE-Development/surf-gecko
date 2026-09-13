package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopProjectiles
import dev.slne.surf.gecko.server.gecko.shop.within
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import java.util.*

private const val RADIUS = 6.0
private const val CLOUD_TICKS = 100
private const val PULSE_TICKS = 5
private const val EFFECT_TICKS = 40
private const val THROW_POWER = 20.0
private const val BURST_PARTICLES = 120
private const val CLOUD_PARTICLES = 45
private const val CORE_PARTICLES = 12
private val BURST_SPREAD = Vec(1.0, 0.6, 1.0)
private val CLOUD_SPREAD = Vec(RADIUS / 2.5, 0.9, RADIUS / 2.5)
private val CORE_SPREAD = Vec(0.6, 0.3, 0.6)

object HiderSmokeBombShopItem : ShopItem {
    override val id = "hider_smoke_bomb"
    override val price = 7
    override val displayName = "Rauchbombe"
    override val description = "Blende die Sucher"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val base = ItemStack.of(Material.FIREWORK_STAR)

    override val displayItem: ItemStack = base
    override val inventoryItem: ItemStack = base.builder().withTag(ShopItem.ID_TAG, id).build()

    private val projectileItem = ItemStack.of(Material.FIREWORK_STAR)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        if (!ShopProjectiles.launch(player, projectileItem, THROW_POWER, ::detonate)) {
            return false
        }

        player.playSound(GeckoSounds.SHOP_SMOKE_BOMB, Sound.Emitter.self())

        return true
    }

    private fun detonate(instance: Instance, position: Pos, thrower: Player) {
        instance.playSound(GeckoSounds.SHOP_SMOKE_BOMB, position.x, position.y, position.z)
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.LARGE_SMOKE,
                position.add(0.0, 0.3, 0.0),
                BURST_SPREAD,
                0.08f,
                BURST_PARTICLES
            )
        )

        val game = GeckoGameManager.findGame(thrower.uuid) ?: return
        val blinded = mutableSetOf<UUID>()

        geckoScope.launch {
            var elapsed = 0

            while (elapsed <= CLOUD_TICKS && instance.isRegistered) {
                emitCloud(instance, position)

                game.activeSeekers().within(instance, position, RADIUS).forEach { seeker ->
                    seeker.addEffect(Potion(PotionEffect.BLINDNESS, 0, EFFECT_TICKS))
                    seeker.addEffect(Potion(PotionEffect.SLOWNESS, 0, EFFECT_TICKS))

                    if (blinded.add(seeker.uuid)) {
                        seeker.sendText {
                            appendPrefix()
                            geckoPrimary("Du wurdest von einer Rauchbombe geblendet!")
                        }
                    }
                }

                delay(PULSE_TICKS.ticks)
                elapsed += PULSE_TICKS
            }
        }
    }

    private fun emitCloud(instance: Instance, position: Pos) {
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.LARGE_SMOKE,
                position.add(0.0, 1.0, 0.0),
                CLOUD_SPREAD,
                0.01f,
                CLOUD_PARTICLES
            )
        )
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.CAMPFIRE_COSY_SMOKE,
                position.add(0.0, 0.4, 0.0),
                CORE_SPREAD,
                0.02f,
                CORE_PARTICLES
            )
        )
    }
}
