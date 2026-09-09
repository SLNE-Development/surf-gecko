package dev.slne.surf.gecko.server.anticheat.check

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.block.Block
import kotlin.math.floor

private const val PROBE_SHRINK = 0.001

object BlockProbe {

    fun intersectsSolid(position: Pos, box: BoundingBox, blockGetter: Block.Getter): Boolean {
        val minX = floor(position.x() + box.minX()).toInt()
        val maxX = floor(position.x() + box.maxX()).toInt()
        val minY = floor(position.y() + box.minY()).toInt()
        val maxY = floor(position.y() + box.maxY()).toInt()
        val minZ = floor(position.z() + box.minZ()).toInt()
        val maxZ = floor(position.z() + box.maxZ()).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val relative = position.sub(x.toDouble(), y.toDouble(), z.toDouble())

                    if (blockGetter.getBlock(x, y, z).collisionShape().intersectBox(relative, box)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun hasGroundSupport(
        position: Pos,
        boundingBox: BoundingBox,
        blockGetter: Block.Getter,
        depth: Double
    ): Boolean {
        val probe = BoundingBox(
            boundingBox.width() - PROBE_SHRINK,
            depth,
            boundingBox.depth() - PROBE_SHRINK
        )

        return intersectsSolid(position.sub(0.0, depth, 0.0), probe, blockGetter)
    }

    fun shrink(boundingBox: BoundingBox, amount: Double): BoundingBox =
        boundingBox.growSymmetrically(-amount, -amount, -amount)
}
