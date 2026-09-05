package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoGameRepository
import dev.slne.surf.gecko.server.gecko.map.GeckoMapManager
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.scoreboard.GeckoScoreboardManager
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

object GeckoGameManager {
    private const val MAX_GAMES = 5

    private val lock = Any()
    private val games = mutableSetOf<GeckoGame>()
    private val starting = AtomicBoolean()
    private lateinit var gameJob: Job

    fun init() {
        gameJob = geckoAsyncScope.runAtFixedRate(3.seconds) {
            testFor()
        }
    }

    fun getGames(): Set<GeckoGame> = synchronized(lock) { games.toSet() }

    suspend fun shutdown() {
        if (::gameJob.isInitialized && gameJob.isActive) {
            gameJob.cancel()
        }

        for (game in getGames()) {
            endGame(game, GeckoGameEndReason.SHUTDOWN)
        }
    }

    fun clearDirtyData(playerUuid: UUID) = synchronized(lock) {
        for (game in games) {
            game.lobbyPlayers.removeAll { it.playerUuid == playerUuid }
            game.gamePlayers.removeAll { it.playerUuid == playerUuid }
        }
    }

    suspend fun handleGameLeave(player: Player) {
        val game = findGame(player.uuid) ?: return
        game.handleLeave(player)
    }

    private suspend fun testFor() {
        if (!requiresGame()) {
            return
        }

        if (!starting.compareAndSet(false, true)) {
            return
        }

        try {
            geckoLogger.info("Starting new game (${getGames().size + 1}/${MAX_GAMES})...")
            startNewGame()
        } finally {
            starting.set(false)
        }
    }

    suspend fun startNewGame(settings: GeckoGameSettings = GeckoGameSettings.default()): GeckoGame {
        val gameId = withContext(Dispatchers.IO) { GeckoGameRepository.saveGame(settings) }
        val instance = GeckoMapManager.prepareMap(settings.map)
        val game = GeckoGame(gameId, settings, instance)

        game.state = GeckoGameState.LOBBY
        synchronized(lock) { games.add(game) }

        GeckoScoreboardManager.createSidebar(game)

        return game
    }

    suspend fun endGame(game: GeckoGame, reason: GeckoGameEndReason) = withContext(Dispatchers.IO) {
        game.gamePlayers.forEach { it.clearRespawnState() }

        GeckoScoreboardManager.removeSidebar(game)

        if (reason.canMovePlayers()) {
            val players = game.players

            players.filterNotNull().forEach {
                requeueOrKick(it)
            }
        }

        synchronized(lock) { games.remove(game) }
        GeckoGameRepository.updateGameEndReason(game, reason)
    }

    fun requeueOrKick(player: Player) {
        val newGame = reserveLobbySlot(player.uuid)

        if (newGame == null) {
            player.kick(buildText {
                appendErrorPrefix()
                error("Es ist aktuell keine Runde verfügbar. Bitte versuche es später erneut.")
            })
            return
        }

        player.instance = newGame.instance
        player.respawnPoint = newGame.settings.map.mapLocations.lobbySpawn
    }

    fun reserveLobbySlot(playerUuid: UUID): GeckoGame? = synchronized(lock) {
        val game = games.filter { it.joinable }.minByOrNull { it.freeSlots } ?: return null

        clearDirtyData(playerUuid)

        game.lobbyPlayers.add(GeckoLobbyPlayer(playerUuid))
        game
    }

    fun findGame(playerUuid: UUID): GeckoGame? = synchronized(lock) {
        games.firstOrNull { game ->
            game.lobbyPlayers.any { it.playerUuid == playerUuid } ||
                    game.gamePlayers.any { it.playerUuid == playerUuid }
        }
    }

    fun releasePlayer(playerUuid: UUID): Unit = synchronized(lock) {
        for (game in games) {
            game.lobbyPlayers.removeAll { it.playerUuid == playerUuid }
            game.gamePlayers.removeAll { it.playerUuid == playerUuid }
        }
    }

    fun requiresGame() = synchronized(lock) {
        games.size < MAX_GAMES && games.none { it.joinable }
    }
}
