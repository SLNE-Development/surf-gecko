package dev.slne.surf.gecko.server.gecko.preriodicBeam

import dev.slne.surf.api.core.util.random
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.BeamEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.ParticleBeam
import dev.slne.surf.gecko.server.gecko.util.GECKO_HIGHLIGHT
import dev.slne.surf.gecko.server.gecko.util.asPlayers
import kotlinx.coroutines.Job
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class PeriodicBeamManager(val game: GeckoGame) {
    private lateinit var job: Job
    var nextBeam: OffsetDateTime = OffsetDateTime.now().plusYears(1)

    private val settings = game.settings
    private val beamChance = settings.beamedPlayerPercentage

    fun start() {
        if (settings.beamIntervall == null || settings.beamDuration == null) {
            return
        }

        nextBeam = OffsetDateTime.now().plus(settings.beamDuration)
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            val now = OffsetDateTime.now()

            if (now.isAfter(nextBeam)) {
                nextBeam = now.plus(settings.beamIntervall)

                val beamedPlayers = game.players.filter {
                    random.nextInt(0, 1) < beamChance
                }

                val beam = ParticleBeam(GECKO_HIGHLIGHT, height = 24.0)
                val beamEffect =
                    BeamEffect(game.seekers.asPlayers, beamedPlayers.map { it.position }, beam)

                beamEffect.playFor(settings.beamDuration.toKotlinDuration())
            }
        }
    }

    fun stop() {
        if (this::job.isInitialized) {
            job.cancel()
        }
    }
}