package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState

class GeckoGame(
    val internalId: ULong,
    val map: GeckoMap
) {
    var state: GeckoGameState = GeckoGameState.OFFLINE
}
