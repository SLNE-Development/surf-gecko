package dev.slne.surf.gecko.server.gecko.lobby

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.display.scoreboard.GeckoScoreboardManager
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.anvil.AnvilLoader
import kotlin.io.path.Path

object GeckoLobby {
    val spawn = Pos(-19.5, 145.0, -8.5, 90f, 0f)
    val npcPos = Pos(-26.5, 145.0, -8.5, -90f, 0f)

    var initialized = false

    lateinit var instance: Instance

    fun createLobby() = MinecraftServer.getInstanceManager().createInstanceContainer(
        AnvilLoader(
            Path("maps/lobby"), key("minecraft:overworld")
        )
    ).also {
        instance = it
        initialized = true
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
