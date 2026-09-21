package dev.slne.surf.gecko.server.gecko.preriodicBeam

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.util.random
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.BeamEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.ParticleBeam
import dev.slne.surf.gecko.server.gecko.util.asPlayers
import dev.slne.surf.gecko.server.util.secureRandom
import kotlinx.coroutines.Job
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class PeriodicBeamManager(val game: GeckoGame) {
    private lateinit var job: Job

    var nextBeam: OffsetDateTime? = null
        private set

    private val settings = game.settings
    private val beamChance = settings.beamedPlayerPercentage

    fun start() {
        val interval = settings.beamIntervall ?: return
        val duration = settings.beamDuration ?: return

        nextBeam = OffsetDateTime.now().plus(interval)
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            val now = OffsetDateTime.now()
            val target = nextBeam ?: return@runAtFixedRate

            if (now.isAfter(target)) {
                nextBeam = now.plus(interval)

                val beamedPlayers = game.hiders.filter {
                    random.nextDouble() < beamChance
                }.ifEmpty {
                    listOf(game.hiders.secureRandom())
                }.asPlayers

                val beam = ParticleBeam(Colors.ERROR, height = 50.0)
                val beamEffect =
                    BeamEffect(game.seekers.asPlayers, beamedPlayers.map { it.position }, beam)

                beamEffect.playFor(duration.toKotlinDuration())
            }
        }
    }

    fun stop() {
        if (this::job.isInitialized) {
            job.cancel()
        }

        nextBeam = null
    }
}
