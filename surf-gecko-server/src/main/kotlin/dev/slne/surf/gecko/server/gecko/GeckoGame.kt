package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.messages.adventure.*
import dev.slne.surf.api.core.util.random
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.common.game.GeckoGameInfo
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.api.minestom.coroutine.ticks
import dev.slne.surf.gecko.server.database.repository.GeckoEventsRepository
import dev.slne.surf.gecko.server.gecko.antiafk.GeckoAntiAfkWatcher
import dev.slne.surf.gecko.server.gecko.display.scoreboard.GeckoScoreboardManager
import dev.slne.surf.gecko.server.gecko.events.GeckoEvent
import dev.slne.surf.gecko.server.gecko.heartbeat.GeckoHeartbeat
import dev.slne.surf.gecko.server.gecko.hotbar.GeckoHotbarItems
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbSpawner
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.game.GeckoPlayerRoleSelector
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.preriodicBeam.PeriodicBeamManager
import dev.slne.surf.gecko.server.gecko.punishment.GeckoGamePunisher
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.gecko.stats.GeckoGameStatsTracker
import dev.slne.surf.gecko.server.gecko.visual.CountdownTitle
import dev.slne.surf.gecko.server.gecko.visual.ScreenFade
import dev.slne.surf.gecko.server.gecko.water.GeckoWaterDamager
import dev.slne.surf.gecko.server.i18n.LocalizedBossBar
import dev.slne.surf.gecko.server.i18n.formatDuration
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.i18n.sendTranslatedActionBar
import dev.slne.surf.gecko.server.i18n.translatable
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.gecko.server.util.secureRandom
import kotlinx.coroutines.*
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.`object`.ObjectContents
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class GeckoGame(
    val internalId: ULong,
    val settings: GeckoGameSettings,
    val instance: Instance
) {
    var state: GeckoGameState = GeckoGameState.OFFLINE
    var countdownSeconds: Int? = null

    private var lobbyCountdownJob: Job? = minestomAsyncScope.runAtFixedRate(1.seconds) {
        updateCountdown()
        tryStart()
        GeckoScoreboardManager.updateSidebar(this@GeckoGame)
    }

    var gameTimerSeconds: Int? = null
    private var gameTimerJob: Job? = null
    private val heartbeat = GeckoHeartbeat(this)
    private val orbSpawner = GeckoOrbSpawner(this)
    private val waterDamager = GeckoWaterDamager(this)
    private val antiAfkWatcher = GeckoAntiAfkWatcher(this)
    private val periodicBeamManager = PeriodicBeamManager(this)

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()

    val seekers get() = gamePlayers.filter { it.role == GeckoGameRole.SEEKER }
    val hiders get() = gamePlayers.filter { it.role == GeckoGameRole.HIDER }
    val spectators get() = gamePlayers.filter { it.role == GeckoGameRole.SPECTATOR }

    val statsTracker = GeckoGameStatsTracker()

    val seekerShopSeed = random.nextLong()
    val hiderShopSeed = random.nextLong()

    var countStats = true

    private val bossBar = LocalizedBossBar(translatable("game.bossbar.waiting.2"), BossBar.Color.PINK)

    private val gameInfoBossBar = LocalizedBossBar(color = BossBar.Color.PINK)

    fun countDownBossBar(seconds: Int) = translatable("game.bossbar.countdown", "seconds" to seconds)

    fun backToLobbyBossBar(seconds: Int) = translatable("game.bossbar.back-to-lobby", "seconds" to seconds)

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = (lobbyPlayers.map { it.playerUuid } + gamePlayers.map { it.playerUuid })
            .distinct()
            .mapNotNull { MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it) }

    fun findGamePlayer(playerUuid: UUID) = gamePlayers.firstOrNull { it.playerUuid == playerUuid }

    fun hideBossBar(player: Player) = bossBar.hide(player)

    fun stopHeartbeat() = heartbeat.stop()
    fun stopOrbSpawner() = orbSpawner.stop()
    fun stopWaterDamager() = waterDamager.stop()
    fun stopAntiAfkWatcher() = antiAfkWatcher.stop()
    fun stopPeriodicBeamManager() = periodicBeamManager.stop()

    fun isTeamDamage(attacker: GeckoGamePlayer, victim: GeckoGamePlayer) =
        attacker.role == victim.role

    private var endingJob: Job? = null
    var endingTimerSeconds: Int? = null

    fun beginEnding(reason: GeckoGameEndReason) {
        state = GeckoGameState.ENDING
        gameTimerJob?.cancel()
        gameTimerJob = null
        heartbeat.stop()
        orbSpawner.stop()
        waterDamager.stop()
        antiAfkWatcher.stop()
        periodicBeamManager.stop()
        stopGameInfoBar()
        endingTimerSeconds = 10

        SocialGroupManager.showAll(this)

        forEachGamePlayer {
            if (it.role == GeckoGameRole.SPECTATOR) {
                it.endSpectating(settings.map)
            }
        }

        forEachPlayer {
            it.inventory.clear()
            GeckoHotbarItems.giveEndingItems(it)
        }

        val result = when (reason) {
            GeckoGameEndReason.SEEKER_WIN -> translatable("game.end.seeker-win")
            GeckoGameEndReason.HIDER_WIN -> translatable("game.end.hider-win")
            else -> translatable("game.end.cancelled")
        }

        sendTranslated(
            "game.end.message",
            "result" to result,
            "seconds" to (endingTimerSeconds ?: 30)
        )

        endingJob = minestomAsyncScope.runAtFixedRate(1.seconds, 1.seconds) {
            val currentEndingSeconds = endingTimerSeconds ?: return@runAtFixedRate
            endingTimerSeconds = currentEndingSeconds - 1

            GeckoScoreboardManager.updateSidebar(this@GeckoGame)
            bossBar.name(backToLobbyBossBar(currentEndingSeconds))

            if (currentEndingSeconds <= 0) {
                forEachPlayer {
                    bossBar.hide(it)
                }
            } else {
                forEachPlayer {
                    bossBar.show(it)
                }
            }


            if (currentEndingSeconds <= 0) {
                GeckoGameManager.endGame(this@GeckoGame, reason)
                endingJob?.cancel()
                endingJob = null
            }
        }
    }

    fun handleDeath(gamePlayer: GeckoGamePlayer) {
        if (!state.isGame() || gamePlayer.awaitingRespawn) {
            return
        }

        if (gamePlayer.role == GeckoGameRole.HIDER) {
            statsTracker.markFound(gamePlayer.playerUuid)
        }

        when (gamePlayer.role) {
            GeckoGameRole.SEEKER -> startSeekerRespawn(gamePlayer)
            GeckoGameRole.HIDER -> handleHiderDeath(gamePlayer)
            GeckoGameRole.SPECTATOR -> Unit
        }
    }

    private fun handleHiderDeath(gamePlayer: GeckoGamePlayer) {
        if (settings.respawnHidersAsSeekers) {
            gamePlayer.role = GeckoGameRole.SEEKER
            statsTracker.markSeekerTeam(gamePlayer.playerUuid)
            startSeekerRespawn(gamePlayer)
            return
        }

        gamePlayer.role = GeckoGameRole.SPECTATOR
        gamePlayer.updateSocialGroup()
        gamePlayer.applyGameMode()
        gamePlayer.applySpeed()
        gamePlayer.applyEquipment()
        gamePlayer.teleportToSpawn(settings.map)

        gamePlayer.player.sendTranslated("game.found.spectator", "role" to GeckoGameRole.SPECTATOR.displayText)
    }

    private fun startSeekerRespawn(gamePlayer: GeckoGamePlayer) {
        gamePlayer.respawnSecondsLeft = settings.seekerRespawnTimeSeconds
        gamePlayer.moveToSeekerLobby(settings.map)

        gamePlayer.player.sendTranslated(
            "game.respawn.message",
            "seconds" to settings.seekerRespawnTimeSeconds,
            "role" to GeckoGameRole.SEEKER.displayText
        )

        gamePlayer.applyGameMode()
        gamePlayer.applySpeed()
        gamePlayer.updateSocialGroup()
        gamePlayer.applyEquipment()
    }

    private fun tickRespawns() {
        gamePlayers.filter { it.awaitingRespawn }.forEach { gamePlayer ->
            val player = gamePlayer.playerOrNull ?: return@forEach
            val secondsLeft = (gamePlayer.respawnSecondsLeft ?: return@forEach) - 1

            if (secondsLeft > 0) {
                gamePlayer.respawnSecondsLeft = secondsLeft

                player.sendTranslatedActionBar("game.respawn.actionbar", "seconds" to secondsLeft)

                return@forEach
            }

            gamePlayer.respawnSecondsLeft = null
            gamePlayer.respawnAsSeeker(settings.map)

            player.sendTranslated("game.respawn.back")
        }
    }

    private fun updateCountdown() {
        if (state != GeckoGameState.LOBBY) {
            return
        }

        val calculated = existingPlayerTime(playerCount, settings)

        if (calculated == null) {
            countdownSeconds = null
            updateCountdownBossBar()
            return
        }

        countdownSeconds = when {
            countdownSeconds == null -> calculated
            countdownSeconds!! <= calculated -> countdownSeconds!! - 1
            else -> calculated
        }

        val secondsLeft = countdownSeconds

        if (secondsLeft != null && secondsLeft in 1..GeckoSounds.COUNTDOWN_SECONDS) {
            players.filterNotNull().forEach {
                it.playSound(GeckoSounds.countdownTick(secondsLeft), Sound.Emitter.self())
                CountdownTitle.show(it, secondsLeft.toString())
            }
        }

        updateCountdownBossBar()
    }

    suspend fun handleLeave(player: Player) {
        if (state.isGame()) {
            val gamePlayer = findGamePlayer(player.uuid) ?: return

            sendTranslated(
                "game.player-left",
                "player" to Component.text(player.username, gamePlayer.role.color, TextDecoration.BOLD)
            )

            statsTracker.markLeft(gamePlayer.playerUuid)
            gamePlayers.removeAll { it.playerUuid == gamePlayer.playerUuid }
            gamePlayer.clearRespawnState()
            gamePlayer.resetSpeed()
            gameInfoBossBar.hide(player)

            if (gamePlayer.role != GeckoGameRole.SPECTATOR) {
                GeckoGamePunisher.punish(player, "punishment.reason.left-game")
            }

            if (checkForGameEnd()) {
                return
            }

            if (gamePlayer.role == GeckoGameRole.SEEKER &&
                gamePlayers.none { it.role == GeckoGameRole.SEEKER }
            ) {
                choseNewRandomSeeker()
            }
        } else {
            val lobbyPlayer = lobbyPlayers.firstOrNull { it.playerUuid == player.uuid } ?: return
            lobbyPlayers.remove(lobbyPlayer)
        }
    }

    private fun choseNewRandomSeeker() {
        val hiders = gamePlayers.filter { it.role == GeckoGameRole.HIDER }

        if (hiders.isEmpty()) {
            beginEnding(GeckoGameEndReason.SEEKER_WIN)
            return
        }

        val newSeeker = hiders.secureRandom()
        newSeeker.role = GeckoGameRole.SEEKER
        statsTracker.markSeekerTeam(newSeeker.playerUuid)
        newSeeker.applyGameMode()
        newSeeker.applySpeed()
        newSeeker.updateSocialGroup()
        newSeeker.applyEquipment()
        newSeeker.teleportToSpawn(settings.map)

        forEachGamePlayer {
            if (it.playerUuid == newSeeker.playerUuid) {
                it.player.sendTranslated("game.seeker-left.self", "role" to GeckoGameRole.SEEKER.displayText)
            } else {
                it.player.sendTranslated(
                    "game.seeker-left.other",
                    "player" to newSeeker.player.username,
                    "role" to GeckoGameRole.SEEKER.displayText
                )
            }
        }
    }

    private suspend fun tryStart() {
        val countdown = countdownSeconds ?: return

        if (countdown <= 0 && state == GeckoGameState.LOBBY) {
            state = GeckoGameState.HIDING
            minestomAsyncScope.launch {
                phaseGame()
            }
        }
    }

    suspend fun phaseGame() {
        if (lobbyCountdownJob != null) {
            lobbyCountdownJob?.cancel()
            lobbyCountdownJob = null
        }

        if (settings != GeckoGameSettings.defaultWithMap(settings.map)) {
            countStats = false
        }

        val roles = GeckoPlayerRoleSelector.selectRoles(
            lobbyPlayers.map { it.playerUuid }.toSet(),
            settings
        )

        lobbyPlayers.forEach {
            val role = roles[it.playerUuid] ?: GeckoGameRole.HIDER
            gamePlayers.add(GeckoGamePlayer(it.playerUuid, role))
        }
        lobbyPlayers.clear()
        statsTracker.beginRound(gamePlayers)


        forEachPlayer {
            ScreenFade.play(it, 10, 20, 15)
            it.playSound(GeckoSounds.PHASE_GAME_TRANSITION, Sound.Emitter.self())
        }
        delay(10.ticks)

        coroutineScope {
            gamePlayers.map { player ->
                async {
                    player.applyGameMode()
                    player.applySpeed()
                    player.applyEquipment()
                    player.updateSocialGroup()
                    player.teleportToSpawn(settings.map)
                    bossBar.hide(player.player)
                    //    player.player.playSound(GeckoSounds.PHASE_GAME, Sound.Emitter.self())
                }
            }.awaitAll()
        }

        minestomAsyncScope.launch {
            delay(35.ticks)
            forEachGamePlayer {
                if (it.playerOrNull != null) {
                    it.sendRoleMessage()
                    //     it.player.playSound(GeckoSounds.ROLE_SELECTED_SOUND, Sound.Emitter.self())
                }
            }
        }

        gameTimerSeconds = settings.roundTimeSeconds
        gameTimerJob = minestomAsyncScope.runAtFixedRate(1.seconds) {
            tickGame()
        }
        heartbeat.start()
        orbSpawner.start()
        antiAfkWatcher.start()
        periodicBeamManager.start()

        if (settings.waterDamage) {
            waterDamager.start()
        }

        startGameInfoBar()

        withContext(Dispatchers.IO) {
            GeckoEventsRepository.logEvent(
                GeckoEvent.GameStart(
                    this@GeckoGame
                )
            )
        }
    }

    private lateinit var gameInfoBarJob: Job
    private fun startGameInfoBar() {
        players.forEach {
            gameInfoBossBar.show(it)
        }

        gameInfoBarJob = minestomAsyncScope.runAtFixedRate(500.milliseconds) {
            forEachPlayer { gameInfoBossBar.show(it) }

            val nextBeam = periodicBeamManager.nextBeam

            if (nextBeam == null) {
                gameInfoBossBar.name(translatable("game.bossbar.info", "map" to settings.map.displayName))
                return@runAtFixedRate
            }

            gameInfoBossBar.name(
                translatable(
                    "game.bossbar.info-beam",
                    "map" to settings.map.displayName,
                    "icon" to Component.`object`(
                        ObjectContents.sprite(
                            key("minecraft", "gui"),
                            key("minecraft", "mob_effect/glowing")
                        )
                    ),
                    "time" to formatDuration(
                        Duration.between(OffsetDateTime.now(), nextBeam)
                            .coerceAtLeast(Duration.ZERO)
                            .toKotlinDuration()
                    )
                )
            )
        }
    }

    fun stopGameInfoBar() {
        if (this::gameInfoBarJob.isInitialized) {
            gameInfoBarJob.cancel()
        }

        forEachPlayer { gameInfoBossBar.hide(it) }
    }

    private var waitingBossBarIndex = 0

    private fun updateCountdownBossBar() {
        val text = if (countdownSeconds != null) {
            countDownBossBar(countdownSeconds!!)
        } else {
            val text = if (waitingBossBarIndex == 0) {
                translatable("game.bossbar.waiting.1")
            } else {
                translatable("game.bossbar.waiting.2")
            }

            waitingBossBarIndex = (waitingBossBarIndex + 1) % 2
            text
        }

        bossBar.name(text)
        players.forEach { player ->
            bossBar.show(player)
        }
    }

    private suspend fun tickGame() {
        val currentTimer = gameTimerSeconds ?: return

        gameTimerSeconds = currentTimer - 1

        GeckoScoreboardManager.updateSidebar(this@GeckoGame)
        tickRespawns()

        if (checkForGameEnd()) {
            return
        }

        val searchStartAt = settings.roundTimeSeconds - settings.hidingTimeSeconds
        val secondsUntilSearch = currentTimer - searchStartAt

        if (state == GeckoGameState.HIDING && secondsUntilSearch in 1..GeckoSounds.COUNTDOWN_SECONDS) {
            broadcastCountdown(secondsUntilSearch, "game.countdown.search")
        }

        if (state == GeckoGameState.HIDING && currentTimer <= searchStartAt) {
            state = GeckoGameState.SEARCHING

            gamePlayers.filter { it.role == GeckoGameRole.SEEKER }.forEach {
                it.player.teleport(settings.map.mapLocations.spawn)
            }

            sendTranslated("game.search.started")

            forEachPlayer {
                it.showTitle {
                    title = it.translate("game.search.title")
                    subtitle = it.translate("game.search.subtitle")
                    times {
                        fadeIn(2)
                        stay(30)
                        fadeOut(10)
                    }
                }

                it.playSound(GeckoSounds.SEARCH_START, Sound.Emitter.self())
                it.playSound(GeckoSounds.COUNTDOWN_FINISHED, Sound.Emitter.self())
            }
        }

        if (state.isGame() && currentTimer in 1..GeckoSounds.COUNTDOWN_SECONDS) {
            broadcastCountdown(currentTimer, "game.countdown.end")
        }

        if (currentTimer <= 0) {
            beginEnding(GeckoGameEndReason.HIDER_WIN)
        }
    }

    private fun broadcastCountdown(secondsLeft: Int, subtitleKey: String) =
        forEachPlayer { player ->
            CountdownTitle.show(player, secondsLeft.toString(), player.translate(subtitleKey))
            player.playSound(GeckoSounds.countdownTick(secondsLeft), Sound.Emitter.self())
        }

    private fun checkForGameEnd(): Boolean {
        if (!state.isGame()) {
            return false
        }

        if (gamePlayers.size <= 1) {
            beginEnding(GeckoGameEndReason.NO_PLAYERS)
            return true
        }

        if (gamePlayers.none { it.role == GeckoGameRole.HIDER }) {
            beginEnding(GeckoGameEndReason.SEEKER_WIN)
            return true
        }

        return false
    }

    private fun existingPlayerTime(
        currentPlayers: Int,
        settings: GeckoGameSettings
    ): Int? {
        if (currentPlayers < settings.minPlayers) {
            return null
        }

        val percentage = currentPlayers.toDouble() / settings.maxPlayers

        return when {
            percentage >= 1.0 -> 10
            percentage >= 0.5 -> (
                    45 - (percentage - 0.5) / 0.5 * 35
                    ).toInt()

            percentage >= 0.1 -> (
                    60 - (percentage - 0.1) / 0.4 * 15
                    ).toInt()

            else -> 60
        }
    }

    fun forEachGamePlayer(action: (player: GeckoGamePlayer) -> Unit) {
        gamePlayers.forEach(action)
    }

    fun forEachPlayer(action: (player: Player) -> Unit) {
        gamePlayers.forEach { player ->
            player.playerOrNull?.let(action)
        }
    }

    fun sendTranslated(key: String, vararg args: Pair<String, Any?>) =
        forEachPlayer { it.sendTranslated(key, *args) }

    val gameInfo
        get() = GeckoGameInfo(
            internalId,
            playerCount,
            settings.maxPlayers,
            settings.map.mapDisplayName
        )
}
