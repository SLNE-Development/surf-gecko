package dev.slne.surf.gecko.server.gecko.heartbeat.effect

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.WorldBorder
import net.minestom.server.network.packet.server.play.WorldBorderWarningReachPacket

private const val MAX_INTENSITY = 0.85

object GeckoScreenEffect {
    fun apply(player: Player, intensity: Double) {
        val border = player.instance?.worldBorder ?: return
        val clamped = intensity.coerceIn(0.0, 1.0) * MAX_INTENSITY

        if (clamped <= 0.0) {
            reset(player)
            return
        }

        val distance = distanceToBorder(border, player.position)
        val warningBlocks = (distance / (1.0 - clamped)).coerceIn(0.0, Int.MAX_VALUE.toDouble())

        player.sendPacket(WorldBorderWarningReachPacket(warningBlocks.toInt()))
    }

    fun reset(player: Player) {
        val border = player.instance?.worldBorder ?: return
        player.sendPacket(border.createWarningReachPacket())
    }

    private fun distanceToBorder(border: WorldBorder, position: Point): Double {
        val radius = border.diameter() / 2.0

        return minOf(
            border.centerX() + radius - position.x(),
            position.x() - (border.centerX() - radius),
            border.centerZ() + radius - position.z(),
            position.z() - (border.centerZ() - radius)
        ).coerceAtLeast(0.0)
    }
}
