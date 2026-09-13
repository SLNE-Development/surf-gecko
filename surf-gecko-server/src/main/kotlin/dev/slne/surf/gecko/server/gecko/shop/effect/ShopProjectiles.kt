package dev.slne.surf.gecko.server.gecko.shop.effect

import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import java.time.Duration

private val LIFETIME: Duration = Duration.ofSeconds(10)

internal object ShopProjectiles {
    val KIND_TAG: Tag<String> = Tag.String("gecko_shop_projectile")

    fun launch(player: Player, kind: String, type: EntityType, power: Double): Boolean {
        val instance = player.instance ?: return false
        val eye = player.position.add(0.0, player.eyeHeight, 0.0)
        val projectile = EntityProjectile(player, type)

        projectile.setTag(KIND_TAG, kind)
        projectile.setInstance(instance, eye)
        projectile.shoot(eye.add(player.position.direction()), power, 0.0)
        projectile.scheduleRemove(LIFETIME)

        return true
    }
}
