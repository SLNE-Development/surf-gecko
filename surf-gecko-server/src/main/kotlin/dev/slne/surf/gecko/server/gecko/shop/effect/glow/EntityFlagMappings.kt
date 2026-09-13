package dev.slne.surf.gecko.server.gecko.shop.effect.glow

import net.minestom.server.entity.metadata.EntityMeta

enum class EntityFlag(val mask: Int, val active: (EntityMeta) -> Boolean) {
    ON_FIRE(0x01, EntityMeta::isOnFire),
    CROUCHING(0x02, EntityMeta::isSneaking),
    SPRINTING(0x08, EntityMeta::isSprinting),
    SWIMMING(0x10, EntityMeta::isSwimming),
    INVISIBLE(0x20, EntityMeta::isInvisible),
    GLOWING(0x40, EntityMeta::isHasGlowingEffect),
    ELYTRA(0x80, EntityMeta::isFlyingWithElytra)
}