package dev.slne.surf.gecko.server.player

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

/**
 * Assisted-inject factory Minestom's player provider is pointed at, so every player is created with
 * its server-side dependencies already wired.
 */
interface GeckoPlayerFactory {

    fun create(
        playerConnection: PlayerConnection,
        gameProfile: GameProfile,
    ): LobbyPlayer
}
