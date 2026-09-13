package dev.slne.surf.gecko.server.gecko.shop.effect

import net.minestom.server.entity.Entity
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket

private const val ENTITY_FLAGS_INDEX = 0
private const val FLAG_ON_FIRE = 0x01
private const val FLAG_CROUCHING = 0x02
private const val FLAG_SPRINTING = 0x08
private const val FLAG_SWIMMING = 0x10
private const val FLAG_INVISIBLE = 0x20
private const val FLAG_GLOWING = 0x40
private const val FLAG_ELYTRA = 0x80

internal object GeckoGlowEffect {
    fun send(viewer: Player, target: Entity, glowing: Boolean) = viewer.sendPacket(
        EntityMetaDataPacket(
            target.entityId,
            mapOf<Int, Metadata.Entry<*>>(
                ENTITY_FLAGS_INDEX to Metadata.Byte(flagsOf(target, glowing))
            )
        )
    )

    private fun flagsOf(target: Entity, glowing: Boolean): Byte {
        val meta = target.entityMeta
        var flags = 0

        if (meta.isOnFire) flags = flags or FLAG_ON_FIRE
        if (meta.isSneaking) flags = flags or FLAG_CROUCHING
        if (meta.isSprinting) flags = flags or FLAG_SPRINTING
        if (meta.isSwimming) flags = flags or FLAG_SWIMMING
        if (meta.isInvisible) flags = flags or FLAG_INVISIBLE
        if (glowing || meta.isHasGlowingEffect) flags = flags or FLAG_GLOWING
        if (meta.isFlyingWithElytra) flags = flags or FLAG_ELYTRA

        return flags.toByte()
    }
}
