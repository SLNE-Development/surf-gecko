package dev.slne.surf.gecko.server.gecko.shop.effect

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderSmokeBombShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerWebGrenadeShopItem
import jakarta.inject.Singleton
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent

@Singleton
class ShopProjectileListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(ProjectileCollideWithBlockEvent::class.java) {
            handleCollision(it.entity, it.collisionPosition)
        }
        node.addListener(ProjectileCollideWithEntityEvent::class.java) {
            handleCollision(it.entity, it.collisionPosition)
        }
    }

    private fun handleCollision(projectile: Entity, position: Pos) {
        if (projectile.isRemoved) {
            return
        }

        val kind = projectile.getTag(ShopProjectiles.KIND_TAG) ?: return
        val instance = projectile.instance ?: return
        val shooter = (projectile as? EntityProjectile)?.shooter as? Player

        projectile.remove()

        when (kind) {
            SeekerWebGrenadeShopItem.PROJECTILE_KIND ->
                SeekerWebGrenadeShopItem.detonate(instance, position)

            HiderSmokeBombShopItem.PROJECTILE_KIND ->
                HiderSmokeBombShopItem.detonate(instance, position, shooter)
        }
    }
}
