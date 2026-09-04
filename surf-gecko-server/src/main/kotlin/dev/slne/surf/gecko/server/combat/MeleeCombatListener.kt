package dev.slne.surf.gecko.server.combat

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.sound
import jakarta.inject.Singleton
import net.kyori.adventure.sound.Sound
import net.minestom.server.ServerFlag
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.tag.Tag

private val lastAttackTickTag = Tag.Long("surf_gecko_last_attack_tick").defaultValue(0L)

@Singleton
class MeleeCombatListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(EntityAttackEvent::class.java, ::handleAttack)
    }

    private fun handleAttack(event: EntityAttackEvent) {
        val attacker = event.entity as? Player ?: return
        val target = event.target as? LivingEntity ?: return

        if (target === attacker) {
            return
        }

        val strength = attacker.attackStrengthScale()
        attacker.setTag(lastAttackTickTag, attacker.aliveTicks)

        val weapon = attacker.itemInMainHand
        val attackDamage = attacker.getAttributeValue(Attribute.ATTACK_DAMAGE).toFloat()
        val critical = strength > CRITICAL_STRENGTH &&
                !attacker.isOnGround &&
                attacker.velocity.y() < 0.0 &&
                !attacker.isSprinting

        val amount = (attackDamage * (WEAK_DAMAGE_RATIO + strength * strength * (1f - WEAK_DAMAGE_RATIO)) +
                weapon.sharpnessBonus() * strength) * if (critical) CRITICAL_MULTIPLIER else 1f

        val applied = target.damageWithInvulnerability(
            Damage(DamageType.PLAYER_ATTACK, attacker, attacker, attacker.position, amount)
        )

        if (applied) {
            val knockbackLevel = weapon.enchantmentLevel(Enchantment.KNOCKBACK) +
                    if (attacker.isSprinting) 1 else 0

            target.takeKnockbackFrom(
                attacker.position.direction(),
                BASE_KNOCKBACK + knockbackLevel * KNOCKBACK_PER_LEVEL
            )
        }

        attacker.playAttackSound(critical, strength, applied)
    }

    private fun Player.attackStrengthScale(): Float {
        val attackSpeed = getAttributeValue(Attribute.ATTACK_SPEED).coerceAtLeast(MIN_ATTACK_SPEED)
        val cooldownTicks = ServerFlag.SERVER_TICKS_PER_SECOND / attackSpeed
        val elapsedTicks = (aliveTicks - getTag(lastAttackTickTag)).toDouble()

        return ((elapsedTicks + 0.5) / cooldownTicks).coerceIn(0.0, 1.0).toFloat()
    }

    private fun ItemStack.sharpnessBonus(): Float {
        val level = enchantmentLevel(Enchantment.SHARPNESS)

        return if (level <= 0) 0f else SHARPNESS_BASE + (level - 1) * SHARPNESS_PER_LEVEL
    }

    private fun Player.playAttackSound(critical: Boolean, strength: Float, applied: Boolean) {
        val instance = instance ?: return
        val soundName = when {
            !applied -> "entity.player.attack.nodamage"
            critical -> "entity.player.attack.crit"
            isSprinting -> "entity.player.attack.knockback"
            strength > STRONG_STRENGTH -> "entity.player.attack.strong"
            else -> "entity.player.attack.weak"
        }

        instance.playSound(
            sound {
                type(key(soundName))
                source(Sound.Source.PLAYER)
            },
            position.x(),
            position.y(),
            position.z()
        )
    }

    private companion object {
        const val WEAK_DAMAGE_RATIO = 0.2f
        const val CRITICAL_STRENGTH = 0.9f
        const val CRITICAL_MULTIPLIER = 1.5f
        const val STRONG_STRENGTH = 0.9f
        const val BASE_KNOCKBACK = 0.4f
        const val SHARPNESS_BASE = 1f
        const val SHARPNESS_PER_LEVEL = 0.5f
        const val MIN_ATTACK_SPEED = 0.1
    }
}
