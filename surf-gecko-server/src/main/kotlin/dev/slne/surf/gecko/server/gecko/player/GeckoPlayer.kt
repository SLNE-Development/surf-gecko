package dev.slne.surf.gecko.server.gecko.player

import net.minestom.server.MinecraftServer
import java.util.*

interface GeckoPlayer {
    val uuid: UUID
    val name: String

    fun hasPermission(permission: String)

    val minestomPlayer get() = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)
}