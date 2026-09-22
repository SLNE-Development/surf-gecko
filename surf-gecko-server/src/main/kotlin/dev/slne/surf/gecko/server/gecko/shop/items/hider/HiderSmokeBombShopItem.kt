package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.grenade.GrenadeImpact
import dev.slne.surf.gecko.server.gecko.shop.effect.grenade.ShopGrenade
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.shop.within
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

object HiderSmokeBombShopItem : ShopItem {
    override val id = "hider_smoke_bomb"
    override val price = 7
    override val displayName = "Rauchbombe"
    override val description = "Blende die Sucher"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)
    override val types = listOf(ShopItemType.HIDER_DEFENSE)

    override val item: ItemStack = ItemStack.of(Material.FIREWORK_STAR)

    private val grenade = ShopGrenade(item, 20.0, ::detonate)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        if (!grenade.throwBy(player)) {
            return false
        }

        player.playSound(GeckoSounds.SHOP_SMOKE_BOMB, Sound.Emitter.self())

        return true
    }

    private fun detonate(impact: GrenadeImpact) {
        val (instance, position, thrower) = impact

        instance.playSound(GeckoSounds.SHOP_SMOKE_BOMB, position.x, position.y, position.z)
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.LARGE_SMOKE,
                position.add(0.0, 0.3, 0.0),
                Vec(1.0, 0.6, 1.0),
                0.08f,
                120
            )
        )

        val game = GeckoGameManager.findGame(thrower.uuid) ?: return
        val blinded = mutableSetOf<UUID>()
        val deadline = TimeSource.Monotonic.markNow() + 5.seconds

        geckoScope.launch {
            while (deadline.hasNotPassedNow() && instance.isRegistered) {
                emitCloud(instance, position)

                game.activeSeekers().within(instance, position, 6.0).forEach { seeker ->
                    seeker.addEffect(Potion(PotionEffect.BLINDNESS, 0, 40))
                    seeker.addEffect(Potion(PotionEffect.SLOWNESS, 0, 40))

                    if (blinded.add(seeker.uuid)) {
                        seeker.sendText {
                            appendPrefix()
                            geckoPrimary("Du wurdest von einer Rauchbombe geblendet!")
                        }
                    }
                }

                delay(5.ticks)
            }
        }
    }

    private fun emitCloud(instance: Instance, position: Pos) {
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.LARGE_SMOKE,
                position.add(0.0, 1.0, 0.0),
                Vec(2.4, 0.9, 2.4),
                0.01f,
                45
            )
        )
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.CAMPFIRE_COSY_SMOKE,
                position.add(0.0, 0.4, 0.0),
                Vec(0.6, 0.3, 0.6),
                0.02f,
                12
            )
        )
    }
}
