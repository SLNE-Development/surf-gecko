package dev.slne.surf.gecko.server.gecko.shop.effect

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import java.time.Duration

private val LIFETIME: Duration = Duration.ofSeconds(15)
private const val SPAWN_FORWARD = 0.5
private const val SPAWN_DROP = 0.2
private const val ARC_LIFT = 3.0
private const val ARM_TICKS = 2

internal object ShopProjectiles {
    fun launch(
        player: Player,
        item: ItemStack,
        power: Double,
        onImpact: (Instance, Pos, Player) -> Unit
    ): Boolean {
        val instance = player.instance ?: return false
        val direction = player.position.direction()
        val origin = player.position
            .add(0.0, player.eyeHeight - SPAWN_DROP, 0.0)
            .add(direction.mul(SPAWN_FORWARD))

        val thrown = ShopThrownItem(item, player, onImpact)

        thrown.setInstance(instance, origin)
        thrown.velocity = direction.mul(power).add(0.0, ARC_LIFT, 0.0)
        thrown.scheduleRemove(LIFETIME)

        return true
    }
}

private class ShopThrownItem(
    item: ItemStack,
    private val thrower: Player,
    private val onImpact: (Instance, Pos, Player) -> Unit
) : ItemEntity(item) {
    private var detonated = false

    init {
        isPickable = false
        isMergeable = false
    }

    override fun movementTick() {
        val before = velocity

        super.movementTick()

        if (detonated || isRemoved || aliveTicks < ARM_TICKS) {
            return
        }

        if (!touchesBlock(before)) {
            return
        }

        val instance = instance ?: return
        val impact = position

        detonated = true
        remove()
        onImpact(instance, impact, thrower)
    }

    private fun touchesBlock(before: Vec) = isOnGround ||
            (before.x != 0.0 && velocity.x == 0.0) ||
            (before.z != 0.0 && velocity.z == 0.0)
}
