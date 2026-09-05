package dev.slne.surf.gecko.server.gecko.lobby

import dev.slne.minestom.lobby.api.key.SurfKey.Companion.key
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.anvil.AnvilLoader
import kotlin.io.path.Path

object GeckoLobby {
    lateinit var instance: Instance
    fun createLobby() = MinecraftServer.getInstanceManager().createInstanceContainer(
        AnvilLoader(
            Path("maps/lobby"), key("minecraft:overworld")
        )
    ).also {
        instance = it
    }

    suspend fun join(player: Player) {
        player.setInstance(instance)
        GeckoGameManager.handleGameLeave(player)
        GeckoGameManager.clearDirtyData(player.uuid)
    }

    fun contains(player: Player) = player.instance == instance
}