package dev.slne.surf.gecko.server.performance.monitor

import java.lang.management.ManagementFactory

object SystemStatistics {
    private val runtime: Runtime = Runtime.getRuntime()
    private val operatingSystem = ManagementFactory.getOperatingSystemMXBean()
            as? com.sun.management.OperatingSystemMXBean

    private var lastCpuTime = -1L
    private var lastSampleAt = 0L

    @Volatile
    var processCpuLoad = 0.0
        private set

    @Volatile
    var systemCpuLoad = -1.0
        private set

    val usedMemory: Long get() = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory: Long get() = runtime.maxMemory()

    fun sample() {
        val bean = operatingSystem ?: return

        val now = System.nanoTime()
        val cpuTime = bean.processCpuTime

        if (cpuTime >= 0L && lastCpuTime >= 0L) {
            val elapsed = now - lastSampleAt
            val available = elapsed.toDouble() * runtime.availableProcessors()

            if (available > 0.0) {
                processCpuLoad = ((cpuTime - lastCpuTime) / available).coerceIn(0.0, 1.0)
            }
        }

        lastCpuTime = cpuTime
        lastSampleAt = now
        systemCpuLoad = bean.cpuLoad
    }
}
