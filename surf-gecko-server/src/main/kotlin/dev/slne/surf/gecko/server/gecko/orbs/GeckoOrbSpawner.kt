package dev.slne.surf.gecko.server.gecko.orbs

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.seconds

class GeckoOrbSpawner(val game: GeckoGame) {
    private lateinit var job: Job

    fun start() {
        job = geckoAsyncScope.runAtFixedRate(30.seconds) {
            val spawns = game.settings.map.mapLocations.orbSpawns
            val playerCount = game.players.size

            val maxAmount = (playerCount * 2).coerceAtMost((spawns.size * 0.15).toInt())
            if (maxAmount <= 0) {
                return@runAtFixedRate
            }

            val amount = if (maxAmount <= playerCount) {
                maxAmount
            } else {
                (playerCount..maxAmount).random()
            }

            spawns.shuffled()
                .take(amount)
                .forEach {
                    GeckoOrb().spawn(game, it)
                }
        }
    }

    fun stop() {
        if (::job.isInitialized) {
            job.cancel()
        }
    }
}