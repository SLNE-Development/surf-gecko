package dev.slne.surf.gecko.server.gecko.util

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer

val Iterable<GeckoGamePlayer>.asPlayers get() = mapNotNull { it.playerOrNull }