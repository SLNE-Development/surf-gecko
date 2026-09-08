package dev.slne.surf.gecko.server.gecko.map.mechanic

import dev.slne.surf.gecko.server.event.MinestomListener

interface GeckoMapMechanic {
    val listeners: List<MinestomListener>

    suspend fun start() = Unit
    suspend fun stop() = Unit
}