package dev.slne.surf.gecko.server.anticheat.check

import net.minestom.server.ServerFlag

private val TICK_MILLIS = 1000L / ServerFlag.SERVER_TICKS_PER_SECOND

class TimerCheck(private val maxDriftMillis: Long) {

    private var lastTickAt = 0L
    private var balance = 0L

    fun record(): Long? {
        val now = System.currentTimeMillis()

        if (lastTickAt == 0L) {
            lastTickAt = now
            return null
        }

        balance += TICK_MILLIS - (now - lastTickAt)
        lastTickAt = now

        if (balance <= maxDriftMillis) {
            balance = balance.coerceAtLeast(-maxDriftMillis)
            return null
        }

        val drift = balance
        balance = 0L

        return drift
    }

    fun reset() {
        lastTickAt = 0L
        balance = 0L
    }
}
