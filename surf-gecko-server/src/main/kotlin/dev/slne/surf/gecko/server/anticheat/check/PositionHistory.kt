package dev.slne.surf.gecko.server.anticheat.check

import net.minestom.server.coordinate.Pos

private const val MAX_ENTRIES = 60

class PositionHistory {

    private val timestamps = ArrayDeque<Long>()
    private val positions = ArrayDeque<Pos>()

    @Synchronized
    fun record(position: Pos) {
        timestamps.addLast(System.currentTimeMillis())
        positions.addLast(position)

        while (positions.size > MAX_ENTRIES) {
            timestamps.removeFirst()
            positions.removeFirst()
        }
    }

    @Synchronized
    fun since(oldest: Long): List<Pos> {
        val result = mutableListOf<Pos>()

        for (index in positions.indices) {
            if (timestamps[index] >= oldest) {
                result.add(positions[index])
            }
        }

        if (result.isEmpty()) {
            positions.lastOrNull()?.let { result.add(it) }
        }

        return result
    }

    @Synchronized
    fun clear() {
        timestamps.clear()
        positions.clear()
    }
}
