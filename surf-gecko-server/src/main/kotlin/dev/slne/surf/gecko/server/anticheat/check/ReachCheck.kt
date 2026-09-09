package dev.slne.surf.gecko.server.anticheat.check

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import kotlin.math.max
import kotlin.math.sqrt

object ReachCheck {

    fun closestDistance(eye: Point, boundingBox: BoundingBox, candidates: List<Pos>): Double {
        var closest = Double.MAX_VALUE

        for (candidate in candidates) {
            val distance = distanceTo(eye, boundingBox, candidate)

            if (distance < closest) {
                closest = distance
            }
        }

        return closest
    }

    private fun distanceTo(eye: Point, boundingBox: BoundingBox, at: Pos): Double {
        val deltaX = axisDistance(eye.x(), at.x() + boundingBox.minX(), at.x() + boundingBox.maxX())
        val deltaY = axisDistance(eye.y(), at.y() + boundingBox.minY(), at.y() + boundingBox.maxY())
        val deltaZ = axisDistance(eye.z(), at.z() + boundingBox.minZ(), at.z() + boundingBox.maxZ())

        return sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
    }

    private fun axisDistance(value: Double, min: Double, max: Double) =
        max(max(min - value, 0.0), value - max)
}
