package dev.slne.surf.gecko.server.gecko.player.game

import net.minestom.server.MinecraftServer
import java.util.*

data class GeckoGamePlayer(
    val playerUuid: UUID,
    var role: GeckoGameRole? = null,
) {
    val player get() = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid)
}
