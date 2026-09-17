package dev.slne.surf.gecko.server.gecko.player.lobby

import java.util.*

data class GeckoLobbyPlayer(
    val playerUuid: UUID
) {
    override fun equals(other: Any?) = other is GeckoLobbyPlayer && other.playerUuid == playerUuid

    override fun hashCode(): Int {
        return playerUuid.hashCode()
    }
}
