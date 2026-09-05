package dev.slne.surf.gecko.server.gecko.lobby

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.scoreboard.GeckoScoreboardManager
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.anvil.AnvilLoader
import kotlin.io.path.Path

object GeckoLobby {
    val spawn = Pos(-311.5, 63.5, -54.5, 45f, 0f)

    lateinit var instance: Instance

    fun createLobby() = MinecraftServer.getInstanceManager().createInstanceContainer(
        AnvilLoader(
            Path("maps/lobby"), key("minecraft:overworld")
        )
    ).also {
        instance = it
    }

    suspend fun join(player: Player) {
        GeckoGameManager.handleGameLeave(player)
        GeckoGameManager.clearDirtyData(player.uuid)
        GeckoScoreboardManager.hideSidebar(player)

        player.respawnPoint = spawn
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = false
        player.inventory.clear()
        player.heal()

        if (contains(player)) {
            player.teleport(spawn)
        } else {
            player.setInstance(instance, spawn)
        }
    }

    fun contains(player: Player) = player.instance == instance
}
