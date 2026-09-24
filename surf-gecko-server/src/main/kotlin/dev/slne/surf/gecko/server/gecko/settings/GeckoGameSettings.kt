package dev.slne.surf.gecko.server.gecko.settings

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.map.GeckoMaps
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

data class GeckoGameSettings(
    val map: GeckoMap,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 16,
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

    val sonarRadius: Double = 20.0,

    val forcedSeekers: Set<UUID> = mutableSetOf(),
    val forcedHiders: Set<UUID> = mutableSetOf()
) {
    companion object {
        fun default() = GeckoGameSettings(
            map = GeckoMaps.random()
        )
    }

    fun mapData(): Map<String, String> = mapOf(
        "map_uuid" to map.mapUuid.toString(),
        "map_name" to map.mapName,
        "min_players" to minPlayers.toString(),
        "max_players" to maxPlayers.toString(),
        "round_time_seconds" to roundTimeSeconds.toString(),
        "hiding_time_seconds" to hidingTimeSeconds.toString(),
        "heartbeat_radius" to heartbeatRadius.toString(),
        "seeker_respawn_time_seconds" to seekerRespawnTimeSeconds.toString(),
        "respawn_hiders_as_seekers" to respawnHidersAsSeekers.toString(),
        "water_damage" to waterDamage.toString(),
        "seeker_speed_factor" to seekerSpeedFactor.toString(),
        "hider_speed_factor" to hiderSpeedFactor.toString(),
        "vent_speed_factor" to ventSpeedFactor.toString(),
        "beam_intervall_ms" to (beamIntervall?.toMillis()?.toString() ?: "UNSET"),
        "beam_duration_ms" to (beamDuration?.toMillis()?.toString() ?: "UNSET"),
        "beamed_player_percentage" to beamedPlayerPercentage.toString(),
        "sonar_radius" to sonarRadius.toString(),
        "forced_seekers" to forcedSeekers.joinToString(","),
        "forced_hiders" to forcedHiders.joinToString(",")
    )
}