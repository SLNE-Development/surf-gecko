package dev.slne.surf.gecko.server.gecko.shop.effect.heart

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.heartbeat.GeckoHeartbeatPulse
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoEffect
import dev.slne.surf.gecko.server.gecko.shop.nearestTo
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class HeartbeatEffect(private val player: Player) :
    GeckoEffect(GeckoHeartbeatPulse.TICK_MILLIS.milliseconds) {

    private var nextBeat = TimeSource.Monotonic.markNow()

    override fun isActive() = player.isOnline
    override fun start() = Unit

    override fun pulse() {
        if (nextBeat.hasNotPassedNow()) {
            return
        }

        val distance = nearestHiderDistance()?.takeIf { it <= 25.0 } ?: return
        val proximity = GeckoHeartbeatPulse.proximityFor(distance, 25.0)

        player.playSound(
            GeckoSounds.heartbeat(
                GeckoHeartbeatPulse.volumeFor(proximity),
                GeckoHeartbeatPulse.pitchFor(proximity)
            ),
            Sound.Emitter.self()
        )

        nextBeat = TimeSource.Monotonic.markNow() +
                GeckoHeartbeatPulse.intervalFor(proximity).milliseconds
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    private fun nearestHiderDistance(): Double? {
        val game = GeckoGameManager.findGame(player.uuid) ?: return null

        return game.activeHiders().nearestTo(player)?.position?.distance(player.position)
    }
}