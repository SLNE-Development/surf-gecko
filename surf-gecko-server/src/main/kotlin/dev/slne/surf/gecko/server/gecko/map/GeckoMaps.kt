package dev.slne.surf.gecko.server.gecko.map

import dev.slne.surf.gecko.server.gecko.map.maps.test.TestMap

object GeckoMaps {
    private val maps = mutableListOf<GeckoMap>()
    fun random() = maps.randomOrNull() ?: error("No maps available")

    init {
        maps.add(
            TestMap
        )
    }
}