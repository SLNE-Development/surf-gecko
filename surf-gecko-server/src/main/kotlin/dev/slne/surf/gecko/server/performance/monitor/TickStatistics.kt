package dev.slne.surf.gecko.server.performance.monitor

import net.minestom.server.ServerFlag

object TickStatistics {
    private const val SAMPLE_COUNT = 100
    private const val NANOS_PER_SECOND = 1_000_000_000.0

    val targetTps = ServerFlag.SERVER_TICKS_PER_SECOND.toDouble()
    val targetMspt = 1000.0 / targetTps

    private val tickTimes = DoubleArray(SAMPLE_COUNT)
    private val tickStamps = LongArray(SAMPLE_COUNT)

    private var nextSample = 0
    private var sampleCount = 0

    @Volatile
    var tps = targetTps
        private set

    @Volatile
    var mspt = 0.0
        private set

    fun record(tickTimeMillis: Double) {
        val now = System.nanoTime()

        tickTimes[nextSample] = tickTimeMillis
        tickStamps[nextSample] = now
        nextSample = (nextSample + 1) % SAMPLE_COUNT

        if (sampleCount < SAMPLE_COUNT) {
            sampleCount++
        }

        var total = 0.0
        for (index in 0 until sampleCount) {
            total += tickTimes[index]
        }

        mspt = total / sampleCount
        tps = measureTps(now)
    }

    private fun measureTps(now: Long): Double {
        if (sampleCount < 2) {
            return targetTps
        }

        val oldest = if (sampleCount < SAMPLE_COUNT) tickStamps[0] else tickStamps[nextSample]
        val elapsed = now - oldest

        if (elapsed <= 0L) {
            return targetTps
        }

        return ((sampleCount - 1) * NANOS_PER_SECOND / elapsed).coerceAtMost(targetTps)
    }
}
