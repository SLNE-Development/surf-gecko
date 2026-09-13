package dev.slne.surf.gecko.server.gecko.shop.effect.grenade

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import java.time.Duration

class ShopGrenade(
    private val item: ItemStack,
    private val power: Double,
    private val onImpact: (GrenadeImpact) -> Unit
) {
    fun throwBy(player: Player): Boolean {
        val instance = player.instance ?: return false
        val direction = player.position.direction()
        val origin = player.position
            .add(0.0, player.eyeHeight - 0.2, 0.0)
            .add(direction.mul(0.5))

        ThrownGrenade(item, player, onImpact).apply {
            setInstance(instance, origin)
            velocity = direction.mul(power).add(0.0, 3.0, 0.0)
            scheduleRemove(Duration.ofSeconds(15))
        }

        return true
    }
}