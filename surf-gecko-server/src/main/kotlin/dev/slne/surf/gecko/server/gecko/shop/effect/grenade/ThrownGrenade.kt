package dev.slne.surf.gecko.server.gecko.shop.effect.grenade

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

class ThrownGrenade(
    item: ItemStack,
    private val thrower: Player,
    private val onImpact: (GrenadeImpact) -> Unit
) : ItemEntity(item) {
    private var detonated = false

    init {
        isPickable = false
        isMergeable = false
    }

    override fun movementTick() {
        val before = velocity

        super.movementTick()

        if (detonated || isRemoved || aliveTicks < 2 || !touchesBlock(before)) {
            return
        }

        val impact = GrenadeImpact(instance ?: return, position, thrower)

        detonated = true

        scheduler().scheduleNextTick {
            remove()
            onImpact(impact)
        }
    }

    private fun touchesBlock(before: Vec) = isOnGround ||
            (before.x != 0.0 && velocity.x == 0.0) ||
            (before.z != 0.0 && velocity.z == 0.0)
}