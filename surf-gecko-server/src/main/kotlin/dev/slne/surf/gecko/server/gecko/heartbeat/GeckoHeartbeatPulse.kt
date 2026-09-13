package dev.slne.surf.gecko.server.gecko.heartbeat

private const val MIN_BEAT_INTERVAL_MILLIS = 250L
private const val MAX_BEAT_INTERVAL_MILLIS = 1400L
private const val MIN_VOLUME = 0.4f
private const val MAX_VOLUME = 1.6f
private const val MIN_PITCH = 0.8f
private const val MAX_PITCH = 1.5f

object GeckoHeartbeatPulse {
    const val TICK_MILLIS = 100L

    fun proximityFor(distance: Double, radius: Double) = (distance / radius).coerceIn(0.0, 1.0)

    fun intervalFor(proximity: Double) =
        (MIN_BEAT_INTERVAL_MILLIS + (MAX_BEAT_INTERVAL_MILLIS - MIN_BEAT_INTERVAL_MILLIS) * proximity).toLong()

    fun volumeFor(proximity: Double) =
        (MAX_VOLUME - (MAX_VOLUME - MIN_VOLUME) * proximity).toFloat()

    fun pitchFor(proximity: Double) =
        (MAX_PITCH - (MAX_PITCH - MIN_PITCH) * proximity).toFloat()

    fun screenIntensityFor(proximity: Double): Double {
        val closeness = 1.0 - proximity

        return closeness * closeness
    }
}
