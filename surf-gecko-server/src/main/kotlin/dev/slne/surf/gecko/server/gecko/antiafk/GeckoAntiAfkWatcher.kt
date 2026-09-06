package dev.slne.surf.gecko.server.gecko.antiafk

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.playtime.api.common.surfPlaytimeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class GeckoAntiAfkWatcher(val game: GeckoGame) {
    private lateinit var watcherJob: Job
    private val afkPlayers = ConcurrentHashMap<UUID, OffsetDateTime>()

    fun start() {
        watcherJob = geckoAsyncScope.runAtFixedRate(1.seconds) {
            game.forEachGamePlayer {
                val player = it.player
                val uuid = player.uuid

                if (surfPlaytimeApi.isPlayerAfk(uuid)) {
                    val afkSince = afkPlayers.computeIfAbsent(uuid) {
                        OffsetDateTime.now()
                    }

                    if (Duration.between(afkSince, OffsetDateTime.now()).seconds >= 5) {
                        player.showTitle {
                            title {
                                error("AFK", TextDecoration.BOLD)
                            }

                            subtitle {
                                geckoSecondary("Bitte bewege dich, sonst wirst du gekickt.")
                            }
                        }

                        player.playSound(true) {
                            type(key("minecraft:block.note_block.bass"))
                            pitch(0.5f)
                        }

                        afkPlayers.remove(uuid)
                        geckoAsyncScope.launch {
                            GeckoLobby.join(player)
                        }
                    }
                } else {
                    afkPlayers.remove(uuid)
                }
            }
        }
    }

    fun stop() {
        if (::watcherJob.isInitialized && watcherJob.isActive) {
            watcherJob.cancel()
        }
    }
}