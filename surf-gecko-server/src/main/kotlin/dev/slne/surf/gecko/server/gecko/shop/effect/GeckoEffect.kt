package dev.slne.surf.gecko.server.gecko.shop.effect

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.TimeSource

abstract class GeckoEffect(private val interval: Duration) {
    abstract fun isActive(): Boolean
    abstract fun start()
    abstract fun pulse()
    abstract fun stop()

    suspend fun playFor(duration: Duration) {
        val deadline = TimeSource.Monotonic.markNow() + duration

        start()

        try {
            while (isActive() && deadline.hasNotPassedNow()) {
                pulse()
                delay(interval)
            }
        } finally {
            stop()
        }
    }
}
