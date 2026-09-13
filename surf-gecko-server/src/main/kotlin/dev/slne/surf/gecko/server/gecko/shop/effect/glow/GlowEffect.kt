package dev.slne.surf.gecko.server.gecko.shop.effect.glow

import dev.slne.surf.gecko.server.antiesp.PlayerXRay
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoEffect
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import kotlin.time.Duration.Companion.milliseconds


class GlowEffect(
    private val viewer: Player,
    private val targets: List<Player>
) : GeckoEffect(250.milliseconds) {

    override fun isActive() = viewer.isOnline && targets.isNotEmpty()

    override fun start() = targets.forEach { PlayerXRay.add(viewer, it) }

    override fun pulse() = outline(true)

    override fun stop() {
        targets.forEach { PlayerXRay.remove(viewer, it) }
        outline(false)
    }

    private fun outline(glowing: Boolean) {
        if (!viewer.isOnline) {
            return
        }

        targets.filter { it.isOnline }.forEach {
            viewer.sendPacket(
                EntityMetaDataPacket(
                    it.entityId,
                    mapOf<Int, Metadata.Entry<*>>(0 to Metadata.Byte(flagsOf(it, glowing)))
                )
            )
        }
    }

    private fun flagsOf(target: Player, glowing: Boolean): Byte {
        val meta = target.entityMeta
        val forced = if (glowing) EntityFlag.GLOWING.mask else 0

        return EntityFlag.entries
            .fold(forced) { flags, flag -> if (flag.active(meta)) flags or flag.mask else flags }
            .toByte()
    }
}
