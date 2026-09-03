package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import kotlinx.coroutines.Job
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
        if(::gameJob.isInitialized && gameJob.isActive) {
            gameJob.cancel()
        }
    }


    private suspend fun testFor() {
        if (requiresGame()) {
            startNewGame()
        }
    }

    suspend fun startNewGame(settings: GeckoGameSettings = GeckoGameSettings.default()) {

    }

    fun requiresGame() = games.size < MAX_GAMES && games.none { it.state.acceptsPlayers() }
}