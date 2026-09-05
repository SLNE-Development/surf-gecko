package dev.slne.surf.gecko.server.gecko.heartbeat

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.heartbeat.effect.GeckoScreenEffect
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import kotlinx.coroutines.Job
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private const val TICK_MILLIS = 100L
private const val MIN_BEAT_INTERVAL_MILLIS = 250L
private const val MAX_BEAT_INTERVAL_MILLIS = 1400L
private const val MIN_VOLUME = 0.4f
private const val MAX_VOLUME = 1.6f
private const val MIN_PITCH = 0.8f
private const val MAX_PITCH = 1.5f

class GeckoHeartbeat(private val game: GeckoGame) {
    private val nextBeatAt = ConcurrentHashMap<UUID, Long>()
    private val affectedScreens = ConcurrentHashMap.newKeySet<UUID>()
    private var job: Job? = null

    fun start() {
        if (job != null) {
            return
        }

        job =
            geckoAsyncScope.runAtFixedRate(TICK_MILLIS.milliseconds, taskName = "gecko-heartbeat") {
                tick()
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        nextBeatAt.clear()
        clearScreens(emptySet())
    }

    private fun tick() {
        if (game.state != GeckoGameState.SEARCHING) {
            return
        }

        val gamePlayers = game.gamePlayers.toList()
        val seekers = gamePlayers
            .filter { it.role == GeckoGameRole.SEEKER && !it.awaitingRespawn }
            .mapNotNull { it.playerOrNull }

        if (seekers.isEmpty()) {
            nextBeatAt.clear()
            clearScreens(emptySet())
            return
        }

        val now = System.currentTimeMillis()
        val nearby = mutableSetOf<UUID>()

        gamePlayers
            .filter { it.role == GeckoGameRole.HIDER && !it.awaitingRespawn }
            .forEach { hider ->
                val player = hider.playerOrNull ?: return@forEach
                val distance = nearestSeekerDistance(player, seekers) ?: return@forEach

                if (distance > game.settings.heartbeatRadius) {
                    nextBeatAt.remove(player.uuid)
                    return@forEach
                }

                val proximity = (distance / game.settings.heartbeatRadius).coerceIn(0.0, 1.0)

                nearby.add(player.uuid)
                affectedScreens.add(player.uuid)
                GeckoScreenEffect.apply(player, screenIntensityFor(proximity))

                val nextBeat = nextBeatAt[player.uuid]

                if (nextBeat != null && now < nextBeat) {
                    return@forEach
                }

                player.playSound(
                    GeckoSounds.heartbeat(volumeFor(proximity), pitchFor(proximity)),
                    Sound.Emitter.self()
                )

                nextBeatAt[player.uuid] = now + intervalFor(proximity)
            }

        clearScreens(nearby)
    }

    private fun clearScreens(keep: Set<UUID>) {
        val outdated = affectedScreens.filterNot { it in keep }

        outdated.forEach { playerUuid ->
            affectedScreens.remove(playerUuid)

            val player = MinecraftServer.getConnectionManager()
                .getOnlinePlayerByUuid(playerUuid) ?: return@forEach

            GeckoScreenEffect.reset(player)
        }
    }

    private fun nearestSeekerDistance(hider: Player, seekers: List<Player>) = seekers
        .filter { it.instance == hider.instance }
        .minOfOrNull { it.position.distance(hider.position) }

    private fun intervalFor(proximity: Double) =
        (MIN_BEAT_INTERVAL_MILLIS + (MAX_BEAT_INTERVAL_MILLIS - MIN_BEAT_INTERVAL_MILLIS) * proximity).toLong()

    private fun volumeFor(proximity: Double) =
        (MAX_VOLUME - (MAX_VOLUME - MIN_VOLUME) * proximity).toFloat()

    private fun pitchFor(proximity: Double) =
        (MAX_PITCH - (MAX_PITCH - MIN_PITCH) * proximity).toFloat()

    private fun screenIntensityFor(proximity: Double): Double {
        val closeness = 1.0 - proximity

        return closeness * closeness
    }
}
