package dev.slne.surf.gecko.server.combat

import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.damage.Damage
import net.minestom.server.item.ItemStack
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import net.minestom.server.tag.Tag

internal const val INVULNERABILITY_TICKS = 10L
internal const val KNOCKBACK_PER_LEVEL = 0.5f

private val lastHurtTickTag = Tag.Long("surf_gecko_last_hurt_tick").defaultValue(0L)
private val lastHurtAmountTag = Tag.Float("surf_gecko_last_hurt_amount").defaultValue(0f)

internal fun LivingEntity.damageWithInvulnerability(damage: Damage): Boolean {
    if (isDead || isInvulnerable) {
        return false
    }

    val amount = reduceByArmor(damage.amount)

    if (amount <= 0f) {
        return false
    }

    if (aliveTicks - getTag(lastHurtTickTag) < INVULNERABILITY_TICKS) {
        val lastAmount = getTag(lastHurtAmountTag)

        if (amount <= lastAmount) {
            return false
        }

        damage.amount = amount - lastAmount
        setTag(lastHurtAmountTag, amount)

        return damage(damage)
    }

    damage.amount = amount
    setTag(lastHurtTickTag, aliveTicks)
    setTag(lastHurtAmountTag, amount)

    return damage(damage)
}

internal fun LivingEntity.takeKnockbackFrom(direction: Vec, strength: Float) {
    if (strength <= 0f) {
        return
    }

    takeKnockback(strength, -direction.x(), -direction.z())
}

internal fun ItemStack.enchantmentLevel(enchantment: RegistryKey<Enchantment>) =
    get(DataComponents.ENCHANTMENTS)?.level(enchantment) ?: 0

private fun LivingEntity.reduceByArmor(amount: Float): Float {
    val armor = getAttributeValue(Attribute.ARMOR).toFloat()

    if (armor <= 0f) {
        return amount
    }

    val toughness = getAttributeValue(Attribute.ARMOR_TOUGHNESS).toFloat()
    val absorbed = (armor - amount / (2f + toughness / 4f)).coerceIn(armor * 0.2f, 20f)

    return amount * (1f - absorbed / 25f)
}
