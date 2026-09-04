package dev.slne.surf.gecko.server.gecko.settings

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.map.GeckoMaps
import java.util.*

private const val DEFAULT_MIN_PLAYERS = 2
private const val DEFAULT_MAX_PLAYERS = 16

data class GeckoGameSettings(
    val map: GeckoMap,
    val minPlayers: Int = DEFAULT_MIN_PLAYERS,
    val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
    val roundTimeSeconds: Int = 300,
    val seekerRespawnTimeSeconds: Int = 30,
    val respawnHidersAsSeekers: Boolean = true,

    val forcedSeekers: Set<UUID> = mutableSetOf(),
    val forcedHiders: Set<UUID> = mutableSetOf()
) {
    companion object {
        fun default() = GeckoGameSettings(
            map = GeckoMaps.random()
        )
    }
}