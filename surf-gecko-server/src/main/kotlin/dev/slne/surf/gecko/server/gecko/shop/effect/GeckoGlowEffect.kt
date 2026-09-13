package dev.slne.surf.gecko.server.gecko.shop.effect

import dev.slne.surf.gecko.server.antiesp.PlayerXRay
import kotlinx.coroutines.delay
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val ENTITY_FLAGS_INDEX = 0
private const val FLAG_ON_FIRE = 0x01
private const val FLAG_CROUCHING = 0x02
private const val FLAG_SPRINTING = 0x08
private const val FLAG_SWIMMING = 0x10
private const val FLAG_INVISIBLE = 0x20
private const val FLAG_GLOWING = 0x40
private const val FLAG_ELYTRA = 0x80

private val REFRESH_INTERVAL = 250.milliseconds

internal object GeckoGlowEffect {
    suspend fun glow(viewer: Player, targets: List<Player>, duration: Duration) {
        if (targets.isEmpty()) {
            return
        }

        targets.forEach { PlayerXRay.add(viewer, it) }

        try {
            val until = System.currentTimeMillis() + duration.inWholeMilliseconds

            while (System.currentTimeMillis() < until && viewer.isOnline) {
                targets.forEach { if (it.isOnline) send(viewer, it, true) }
                delay(REFRESH_INTERVAL)
            }
        } finally {
            targets.forEach { PlayerXRay.remove(viewer, it) }

            if (viewer.isOnline) {
                targets.forEach { if (it.isOnline) send(viewer, it, false) }
            }
        }
    }

    private fun send(viewer: Player, target: Entity, glowing: Boolean) = viewer.sendPacket(
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
