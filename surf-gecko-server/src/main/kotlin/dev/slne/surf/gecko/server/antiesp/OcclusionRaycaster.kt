package dev.slne.surf.gecko.server.antiesp

import net.minestom.server.entity.Player
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import kotlin.math.abs
import kotlin.math.floor

private const val MAX_DISTANCE = 64.0
private const val MIN_DISTANCE = 2.0
private const val MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE
private const val MIN_DISTANCE_SQUARED = MIN_DISTANCE * MIN_DISTANCE
private const val FEET_OFFSET = 0.1
private const val CORNER_INSET = 0.05
private const val MAX_STEPS = 512

@Suppress("UnstableApiUsage")
internal class OcclusionRaycaster(private val instance: Instance) : AutoCloseable {
    private val minY = instance.cachedDimensionType.minY()
    private val maxY = minY + instance.cachedDimensionType.height()

    private var chunk: Chunk? = null
    private var chunkX = Int.MAX_VALUE
    private var chunkZ = Int.MAX_VALUE

    fun hasLineOfSight(viewer: Player, target: Player): Boolean {
        val from = viewer.position
        val to = target.position

        val dx = to.x() - from.x()
        val dy = to.y() - from.y()
        val dz = to.z() - from.z()
        val distanceSquared = dx * dx + dy * dy + dz * dz

        if (distanceSquared !in MIN_DISTANCE_SQUARED..MAX_DISTANCE_SQUARED) {
            return true
        }

        val eyeX = from.x()
        val eyeY = from.y() + viewer.eyeHeight
        val eyeZ = from.z()

        if (isClear(eyeX, eyeY, eyeZ, to.x(), to.y() + target.eyeHeight, to.z())) {
            return true
        }

        if (isClear(eyeX, eyeY, eyeZ, to.x(), to.y() + FEET_OFFSET, to.z())) {
            return true
        }

        val box = target.boundingBox
        val offset = box.width() / 2 - CORNER_INSET
        val cornerY = to.y() + box.height() * 0.5

        return isClear(eyeX, eyeY, eyeZ, to.x() - offset, cornerY, to.z() - offset) ||
                isClear(eyeX, eyeY, eyeZ, to.x() - offset, cornerY, to.z() + offset) ||
                isClear(eyeX, eyeY, eyeZ, to.x() + offset, cornerY, to.z() - offset) ||
                isClear(eyeX, eyeY, eyeZ, to.x() + offset, cornerY, to.z() + offset)
    }

    override fun close() = releaseChunk()

    private fun isClear(
        originX: Double,
        originY: Double,
        originZ: Double,
        targetX: Double,
        targetY: Double,
        targetZ: Double,
    ): Boolean {
        val deltaX = targetX - originX
        val deltaY = targetY - originY
        val deltaZ = targetZ - originZ

        var x = floor(originX).toInt()
        var y = floor(originY).toInt()
        var z = floor(originZ).toInt()

        val stepX = if (deltaX > 0.0) 1 else -1
        val stepY = if (deltaY > 0.0) 1 else -1
        val stepZ = if (deltaZ > 0.0) 1 else -1

        val spanX = span(deltaX)
        val spanY = span(deltaY)
        val spanZ = span(deltaZ)

        var nextX = boundary(originX, deltaX, x)
        var nextY = boundary(originY, deltaY, y)
        var nextZ = boundary(originZ, deltaZ, z)

        var steps = 0

        while (steps++ < MAX_STEPS) {
            if (nextX < nextY) {
                if (nextX < nextZ) {
                    if (nextX > 1.0) return true
                    x += stepX
                    nextX += spanX
                } else {
                    if (nextZ > 1.0) return true
                    z += stepZ
                    nextZ += spanZ
                }
            } else {
                if (nextY < nextZ) {
                    if (nextY > 1.0) return true
                    y += stepY
                    nextY += spanY
                } else {
                    if (nextZ > 1.0) return true
                    z += stepZ
                    nextZ += spanZ
                }
            }

            if (occludes(x, y, z)) return false
        }

        return true
    }

    private fun occludes(x: Int, y: Int, z: Int): Boolean {
        if (y < minY || y >= maxY) return false

        val targetChunkX = x shr 4
        val targetChunkZ = z shr 4
        var current = chunk

        if (current == null || targetChunkX != chunkX || targetChunkZ != chunkZ) {
            releaseChunk()
            current = instance.getChunk(targetChunkX, targetChunkZ) ?: return false
            current.lockReadLock()
            chunk = current
            chunkX = targetChunkX
            chunkZ = targetChunkZ
        }

        return current.getBlock(x, y, z, Block.Getter.Condition.TYPE)?.occludes() == true
    }

    private fun releaseChunk() {
        chunk?.unlockReadLock()
        chunk = null
        chunkX = Int.MAX_VALUE
        chunkZ = Int.MAX_VALUE
    }

    private fun span(delta: Double) = if (delta != 0.0) abs(1.0 / delta) else Double.MAX_VALUE

    private fun boundary(origin: Double, delta: Double, voxel: Int) = when {
        delta > 0.0 -> (voxel + 1 - origin) / delta
        delta < 0.0 -> (voxel - origin) / delta
        else -> Double.MAX_VALUE
    }
}
