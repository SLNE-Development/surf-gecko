package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.game.GeckoPlayerRoleSelector
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import kotlin.time.Duration.Companion.seconds

class GeckoGame(
    val internalId: ULong,
    val settings: GeckoGameSettings,
    val instance: Instance
) {
    var state: GeckoGameState = GeckoGameState.OFFLINE
    var countdownSeconds: Int? = null

    private var lobbyCountdownJob: Job? = geckoAsyncScope.runAtFixedRate(1.seconds) {
        updateCountdown()
        tryStart()
    }

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()

    val countdownBossBar1 = bossBar {
        name {
            primary("Warte auf weitere Spieler.")
        }
    }

    val countdownBossBar2 = bossBar {
        name {
            primary("Warte auf weitere Spieler..")
        }
    }

    val countdownBossBar3 = bossBar {
        name {
            primary("Warte auf weitere Spieler...")
        }
    }

    fun countDownBossBar(seconds: Int) = bossBar {
        name {
            primary("Spiel startet in ")
            variableValue(seconds)
            primary(" Sekunden.")
        }
    }

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = lobbyPlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        } + gamePlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        }


    private suspend fun joinLobby(player: Player): Boolean {
        if (!joinable) {
            return false
        }

        GeckoGameManager.clearDirtyData(player.uuid)

        lobbyPlayers.add(GeckoLobbyPlayer(player.uuid))
        player.setInstance(instance).await()
        player.teleport(settings.map.mapLocations.lobbySpawn).await()

        return true
    }

    private fun updateCountdown() {
        if (state != GeckoGameState.LOBBY) {
            return
        }

        val calculated = existingPlayerTime(playerCount, settings)

        if (calculated == null) {
            countdownSeconds = null
            return
        }

        if (countdownSeconds == null) {
            countdownSeconds = calculated
            return
        }

        countdownSeconds = calculated
    }

    private suspend fun tryStart() {
        val countdown = countdownSeconds ?: return

        if (countdown <= 0) {
            phaseGame()
        }
    }

    suspend fun phaseGame() {
        val roles = GeckoPlayerRoleSelector.selectRoles(
            lobbyPlayers.map { it.playerUuid }.toSet(),
            settings
        )

        lobbyPlayers.forEach {
            val role = roles[it.playerUuid] ?: GeckoGameRole.HIDER
            gamePlayers.add(GeckoGamePlayer(it.playerUuid, role))
        }

        coroutineScope {
            gamePlayers.map { player ->
                async {
                    player.applyGameMode()
                    player.applyEquipment()
                    player.teleportToSpawn(settings.map)
                }
            }.awaitAll()
        }

        state = GeckoGameState.GAME
    }

    private fun existingPlayerTime(
        currentPlayers: Int,
        settings: GeckoGameSettings
    ): Int? {
        if (currentPlayers < settings.minPlayers) {
            return null
        }

        val percentage = currentPlayers.toDouble() / settings.maxPlayers

        return when {
            percentage >= 1.0 -> 10
            percentage >= 0.5 -> (
                    45 - (percentage - 0.5) / 0.5 * 35
                    ).toInt()

            percentage >= 0.1 -> (
                    60 - (percentage - 0.1) / 0.4 * 15
                    ).toInt()

            else -> 60
        }
    }
}
