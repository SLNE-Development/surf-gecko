package dev.slne.surf.gecko.server.combat

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sound
import jakarta.inject.Singleton
import net.kyori.adventure.sound.Sound
import net.minestom.server.ServerFlag
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.projectile.AbstractArrowMeta
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.item.PlayerCancelItemUseEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.tag.Tag
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

private val arrowDamageTag = Tag.Double("surf_gecko_arrow_damage").defaultValue(2.0)
private val arrowPunchTag = Tag.Integer("surf_gecko_arrow_punch").defaultValue(0)

@Singleton
class BowCombatListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerCancelItemUseEvent::class.java, ::handleBowRelease)
        node.addListener(ProjectileCollideWithEntityEvent::class.java, ::handleArrowHit)
        node.addListener(ProjectileCollideWithBlockEvent::class.java, ::handleArrowLanding)
    }

    private fun handleBowRelease(event: PlayerCancelItemUseEvent) {
        val bow = event.itemStack

        if (bow.material() != Material.BOW) {
            return
        }

        val player = event.player
        val instance = player.instance ?: return
        val power = bowPower(event.useDuration)

        if (power < MIN_POWER || !player.consumeArrow(bow)) {
            return
        }

        val powerLevel = bow.enchantmentLevel(Enchantment.POWER)
        val arrow = EntityProjectile(player, EntityType.ARROW)

        (arrow.entityMeta as AbstractArrowMeta).isCritical = power >= MAX_POWER
        arrow.setTag(
            arrowDamageTag,
            BASE_ARROW_DAMAGE + if (powerLevel > 0) powerLevel * POWER_PER_LEVEL + POWER_BASE else 0.0
        )
        arrow.setTag(arrowPunchTag, bow.enchantmentLevel(Enchantment.PUNCH))

        val origin = player.position.add(0.0, player.eyeHeight - SPAWN_OFFSET, 0.0)

        arrow.setInstance(instance, origin).thenRun {
            arrow.velocity = player.position.direction().withSpread()
                .mul((power * ARROW_BLOCKS_PER_TICK * ServerFlag.SERVER_TICKS_PER_SECOND).toDouble())
            arrow.scheduleRemove(Duration.ofSeconds(ARROW_LIFETIME_SECONDS))
        }

        player.playBowSound(power)
    }

    private fun handleArrowHit(event: ProjectileCollideWithEntityEvent) {
        val arrow = event.entity as? EntityProjectile ?: return

        if (arrow.entityType != EntityType.ARROW) {
            return
        }

        val target = event.target as? LivingEntity ?: return

        if (target.isDead || target.isInvulnerable) {
            event.isCancelled = true
            return
        }

        val shooter = arrow.shooter
        val blocksPerTick = arrow.velocity.length() / ServerFlag.SERVER_TICKS_PER_SECOND
        val base = ceil(blocksPerTick * arrow.getTag(arrowDamageTag)).toInt().coerceAtLeast(0)
        val amount = if ((arrow.entityMeta as AbstractArrowMeta).isCritical) {
            base + ThreadLocalRandom.current().nextInt(base / 2 + 2)
        } else {
            base
        }

        val applied = target.damageWithInvulnerability(
            Damage(DamageType.ARROW, arrow, shooter, arrow.position, amount.toFloat())
        )

        if (applied) {
            target.takeKnockbackFrom(
                arrow.velocity.normalize().neg(),
                BASE_ARROW_KNOCKBACK + arrow.getTag(arrowPunchTag) * KNOCKBACK_PER_LEVEL
            )

            (shooter as? Player)?.playSound(true) {
                type(key("entity.arrow.hit_player"))
                source(Sound.Source.PLAYER)
            }
        }

        arrow.remove()
    }

    private fun handleArrowLanding(event: ProjectileCollideWithBlockEvent) {
        val arrow = event.entity as? EntityProjectile ?: return

        if (arrow.entityType != EntityType.ARROW) {
            return
        }

        arrow.remove()
    }

    private fun bowPower(useDurationTicks: Long): Float {
        val seconds = useDurationTicks.toFloat() / ServerFlag.SERVER_TICKS_PER_SECOND

        return ((seconds * seconds + seconds * 2f) / 3f).coerceAtMost(MAX_POWER)
    }

    private fun Vec.withSpread(): Vec {
        val random = ThreadLocalRandom.current()

        return Vec(
            x() + random.nextGaussian() * SPREAD,
            y() + random.nextGaussian() * SPREAD,
            z() + random.nextGaussian() * SPREAD
        )
    }

    private fun Player.consumeArrow(bow: ItemStack): Boolean {
        if (gameMode == GameMode.CREATIVE || bow.enchantmentLevel(Enchantment.INFINITY) > 0) {
            return true
        }

        val slot = (0 until inventory.size)
            .firstOrNull { inventory.getItemStack(it).material() == Material.ARROW } ?: return false

        inventory.setItemStack(slot, inventory.getItemStack(slot).consume(1))

        return true
    }

    private fun Player.playBowSound(power: Float) {
        val instance = instance ?: return
        val random = ThreadLocalRandom.current()

        instance.playSound(
            sound {
                type(key("entity.arrow.shoot"))
                source(Sound.Source.PLAYER)
                pitch(1f / (random.nextFloat() * 0.4f + 1.2f) + power * 0.5f)
            },
            position.x(),
            position.y(),
            position.z()
        )
    }

    private companion object {
        const val MIN_POWER = 0.1f
        const val MAX_POWER = 1f
        const val ARROW_BLOCKS_PER_TICK = 3f
        const val SPAWN_OFFSET = 0.1
        const val SPREAD = 0.0075
        const val BASE_ARROW_DAMAGE = 2.0
        const val POWER_PER_LEVEL = 0.5
        const val POWER_BASE = 0.5
        const val BASE_ARROW_KNOCKBACK = 0.4f
        const val ARROW_LIFETIME_SECONDS = 60L
    }
}
