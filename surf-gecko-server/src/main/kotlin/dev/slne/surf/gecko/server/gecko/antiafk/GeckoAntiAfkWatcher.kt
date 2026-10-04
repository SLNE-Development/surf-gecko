package dev.slne.surf.gecko.server.gecko.antiafk

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.visual.ScreenFade
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.playtime.api.common.surfPlaytimeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Player
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class GeckoAntiAfkWatcher(val game: GeckoGame) {
    private lateinit var watcherJob: Job
    private val afkPlayers = ConcurrentHashMap<UUID, OffsetDateTime>()

    fun start() {
        watcherJob = minestomAsyncScope.runAtFixedRate(TICK_INTERVAL) {
            game.forEachGamePlayer {
                val player = it.playerOrNull ?: return@forEachGamePlayer
                val uuid = player.uuid

                if (!surfPlaytimeApi.isPlayerAfk(uuid)) {
                    afkPlayers.remove(uuid)
                    return@forEachGamePlayer
                }

                val afkSince = afkPlayers.computeIfAbsent(uuid) { OffsetDateTime.now() }
                val afkMillis = Duration.between(afkSince, OffsetDateTime.now()).toMillis()

                if (afkMillis < GRACE_MILLIS) {
                    return@forEachGamePlayer
                }

                val millisLeft = GRACE_MILLIS + WARNING_MILLIS - afkMillis

                if (millisLeft > 0) {
                    warn(player, ceil(millisLeft / 1000.0).toLong())
                    return@forEachGamePlayer
                }

                afkPlayers.remove(uuid)
                minestomAsyncScope.launch {
                    ScreenFade.transition(listOf(player))
                    GeckoLobby.join(player)
                }
            }
        }
    }

    private fun warn(player: Player, secondsLeft: Long) {
        player.showTitle {
            title = player.translate("game.afk.title")
            subtitle = player.translate("game.afk.subtitle", "seconds" to secondsLeft)

            times {
                fadeIn(0)
                stay(20)
                fadeOut(0)
            }
        }

        player.playSound(true) {
            type(key("minecraft:block.note_block.pling"))
            pitch(2f)
        }
    }

    fun stop() {
        if (::watcherJob.isInitialized && watcherJob.isActive) {
            watcherJob.cancel()
        }
    }

    private companion object {
        val TICK_INTERVAL = 150.milliseconds
        const val GRACE_MILLIS = 5000L
        const val WARNING_MILLIS = 15000L
    }
}