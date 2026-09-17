package dev.slne.surf.gecko.server.performance

import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import java.util.function.Consumer

object EntityViewerLookup {
    private const val DIRECT_SCAN_COST_FACTOR = 3L

    fun interface EntityPositionResolver {
        fun resolve(entity: Entity): Point?
    }

    @JvmStatic
    fun useDirectScan(candidateCount: Int, chunkRange: Int): Boolean {
        if (chunkRange <= 0) return false
        val side = 2L * chunkRange + 1L
        return candidateCount * DIRECT_SCAN_COST_FACTOR < side * side
    }

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    @JvmStatic
    fun <T : Entity> directScan(
        candidates: Set<T>,
        resolver: EntityPositionResolver,
        centerChunkX: Int,
        centerChunkZ: Int,
        chunkRange: Int,
        query: Consumer<T>,
    ) {
        val minX = centerChunkX - chunkRange
        val maxX = centerChunkX + chunkRange
        val minZ = centerChunkZ - chunkRange
        val maxZ = centerChunkZ + chunkRange

        for (candidate in candidates) {
            val position = resolver.resolve(candidate) ?: continue

            val chunkX = position.chunkX()
            if (chunkX < minX || chunkX > maxX) continue

            val chunkZ = position.chunkZ()
            if (chunkZ < minZ || chunkZ > maxZ) continue

            query.accept(candidate)
        }
    }

    @JvmStatic
    fun chunksInRangeRectangular(
        chunkX: Int,
        chunkZ: Int,
        chunkRange: Int,
        consumer: ChunkRange.ChunkConsumer,
    ) {
        val minX = chunkX - chunkRange
        val maxX = chunkX + chunkRange
        val minZ = chunkZ - chunkRange
        val maxZ = chunkZ + chunkRange
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                consumer.accept(x, z)
            }
        }
    }
}