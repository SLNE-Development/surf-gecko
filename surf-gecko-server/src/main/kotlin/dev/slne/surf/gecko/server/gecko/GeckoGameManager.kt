package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoGameRepository
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

object GeckoGameManager {
    private const val MAX_GAMES = 5

    private val games = mutableSetOf<GeckoGame>()
    private lateinit var gameJob: Job

    fun init() {
        gameJob = geckoAsyncScope.runAtFixedRate(3.seconds) {
            testFor()
        }
    }

    fun shutdown() {
        if (::gameJob.isInitialized && gameJob.isActive) {
            gameJob.cancel()
        }
    }


    private suspend fun testFor() {
        if (requiresGame()) {
            geckoLogger.info("Starting new game (${games.size + 1}/${MAX_GAMES})...")
            startNewGame()
        }
    }

    suspend fun startNewGame(settings: GeckoGameSettings = GeckoGameSettings.default()): GeckoGame =
        withContext(Dispatchers.IO) {
            val gameId = GeckoGameRepository.saveGame(settings)
            val game = GeckoGame(gameId, settings)

            games.add(game)
            return@withContext game
        }


    suspend fun endGame(game: GeckoGame, reason: GeckoGameEndReason) = withContext(Dispatchers.IO) {
        games.remove(game)
        GeckoGameRepository.updateGameEndReason(game, reason)
    }

    fun requiresGame() = games.size < MAX_GAMES && games.none { it.state.acceptsPlayers() }
}