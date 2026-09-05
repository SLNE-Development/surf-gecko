package dev.slne.surf.gecko.map.creator.draft

import org.bukkit.Location
import org.bukkit.World

data class DraftPos(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
) {
    fun toLocation(world: World) = Location(world, x, y, z, yaw, pitch)

    fun distanceSquaredTo(other: Location) =
        (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) + (z - other.z) * (z - other.z)

    companion object {
        fun of(location: Location) = DraftPos(
            location.x,
            location.y,
            location.z,
            normalizeYaw(location.yaw),
            location.pitch,
        )

        fun ofBlockCenter(location: Location, yaw: Float, pitch: Float) = DraftPos(
            location.blockX + 0.5,
            location.blockY.toDouble(),
            location.blockZ + 0.5,
            normalizeYaw(yaw),
            pitch,
        )

        private fun normalizeYaw(yaw: Float): Float {
            var normalized = yaw % 360f
            if (normalized > 180f) normalized -= 360f
            if (normalized <= -180f) normalized += 360f
            return normalized
        }
    }
}
