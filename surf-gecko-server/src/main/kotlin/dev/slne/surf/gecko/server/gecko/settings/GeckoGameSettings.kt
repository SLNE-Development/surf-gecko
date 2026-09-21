package dev.slne.surf.gecko.server.gecko.settings

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.map.GeckoMaps
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private const val DEFAULT_MIN_PLAYERS = 2
private const val DEFAULT_MAX_PLAYERS = 16

data class GeckoGameSettings(
    val map: GeckoMap,
    val minPlayers: Int = DEFAULT_MIN_PLAYERS,
    val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
    val roundTimeSeconds: Int = 600,
    val hidingTimeSeconds: Int = 60,
    val heartbeatRadius: Double = 15.0,
    val seekerRespawnTimeSeconds: Int = 30,
    val respawnHidersAsSeekers: Boolean = true,
    val waterDamage: Boolean = true,

    val seekerSpeedFactor: Double = 1.05,
    val hiderSpeedFactor: Double = 1.0,
    val ventSpeedFactor: Double = 1.2,

    val beamIntervall: Duration? = 2.minutes.toJavaDuration(),
    val beamDuration: Duration? = 7.5.seconds.toJavaDuration(),
    val beamedPlayerPercentage: Double = 0.25,

    val forcedSeekers: Set<UUID> = mutableSetOf(),
    val forcedHiders: Set<UUID> = mutableSetOf()
) {
    companion object {
        fun default() = GeckoGameSettings(
            map = GeckoMaps.random()
        )
    }
}