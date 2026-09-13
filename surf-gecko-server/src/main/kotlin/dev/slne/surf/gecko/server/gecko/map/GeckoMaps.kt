package dev.slne.surf.gecko.server.gecko.map

import dev.slne.surf.gecko.server.gecko.map.maps.scientistcity.ScientistCityMap
import dev.slne.surf.gecko.server.util.secureRandomOrNull

object GeckoMaps {
    private val maps = mutableListOf<GeckoMap>()
    fun random() = maps.secureRandomOrNull() ?: error("No maps available")

    init {
        maps.add(ScientistCityMap)
    }
}