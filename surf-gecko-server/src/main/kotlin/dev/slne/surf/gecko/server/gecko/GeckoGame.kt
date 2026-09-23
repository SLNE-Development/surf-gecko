package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.*
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.common.game.GeckoGameInfo
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.antiafk.GeckoAntiAfkWatcher
import dev.slne.surf.gecko.server.gecko.display.scoreboard.GeckoScoreboardManager
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
import dev.slne.surf.gecko.server.gecko.util.*
import dev.slne.surf.gecko.server.gecko.visual.CountdownTitle
import dev.slne.surf.gecko.server.gecko.visual.ScreenFade
import dev.slne.surf.gecko.server.gecko.water.GeckoWaterDamager
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

    private var lobbyCountdownJob: Job? = geckoAsyncScope.runAtFixedRate(1.seconds) {
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

    val countdownBossBar2 = buildText {
        geckoPrimary("Warte auf weitere Spieler.. ".toSmallCaps())
    }

    val countdownBossBar3 = buildText {
        geckoPrimary("Warte auf weitere Spieler...".toSmallCaps())
    }

    private val bossBar = bossBar {
        name {
            append(countdownBossBar3)
        }
        color = BossBar.Color.PINK
    }

    private val gameInfoBossBar = bossBar {
        color = BossBar.Color.PINK
    }

    fun countDownBossBar(seconds: Int) = buildText {
        geckoPrimary("Das Spiel startet in ".toSmallCaps())
        geckoSecondary(seconds.toString())
        geckoPrimary(" Sekunden.".toSmallCaps())
    }

    fun backToLobbyBossBar(seconds: Int) = buildText {
        geckoPrimary("Zurück zur Lobby in ".toSmallCaps())
        geckoSecondary(seconds.toString())
        geckoPrimary(" Sekunden...".toSmallCaps())
    }

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = (lobbyPlayers.map { it.playerUuid } + gamePlayers.map { it.playerUuid })
            .distinct()
            .mapNotNull { MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it) }

    fun findGamePlayer(playerUuid: UUID) = gamePlayers.firstOrNull { it.playerUuid == playerUuid }

    fun hideBossBar(player: Player) = player.hideBossBar(bossBar)

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

        sendText {
            appendNewline()
            appendPrefix()
            when (reason) {
                GeckoGameEndReason.SEEKER_WIN -> {
                    text(
                        "Die Sucher haben gewonnen",
                        GeckoGameRole.SEEKER.color,
                        TextDecoration.BOLD
                    )
                }

                GeckoGameEndReason.HIDER_WIN -> {
                    text(
                        "Die Verstecker haben gewonnen",
                        GeckoGameRole.HIDER.color,
                        TextDecoration.BOLD
                    )
                }

                else -> {
                    error("Das Spiel wurde beendet", TextDecoration.BOLD)
                }
            }
            appendNewline()
            appendPrefix()
            geckoSecondary("Du wirst in ".toSmallCaps())
            geckoHighlight((endingTimerSeconds ?: 30).toString())
            geckoSecondary(" Sekunden in")
            appendNewline()
            appendPrefix()
            geckoSecondary("die Lobby geschickt.")
        }

        endingJob = geckoAsyncScope.runAtFixedRate(1.seconds, 1.seconds) {
            val currentEndingSeconds = endingTimerSeconds ?: return@runAtFixedRate
            endingTimerSeconds = currentEndingSeconds - 1

            GeckoScoreboardManager.updateSidebar(this@GeckoGame)
            bossBar.name(backToLobbyBossBar(currentEndingSeconds))

            if (currentEndingSeconds <= 0) {
                forEachPlayer {
                    it.hideBossBar(bossBar)
                }
            } else {
                forEachPlayer {
                    it.showBossBar(bossBar)
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

        gamePlayer.player.sendText {
            appendPrefix()
            geckoPrimary("Du wurdest gefunden und bist nun ")
            append(GeckoGameRole.SPECTATOR.displayText)
            geckoPrimary(".")
        }
    }

    private fun startSeekerRespawn(gamePlayer: GeckoGamePlayer) {
        gamePlayer.respawnSecondsLeft = settings.seekerRespawnTimeSeconds
        gamePlayer.moveToSeekerLobby(settings.map)

        gamePlayer.player.sendText {
            appendPrefix()
            geckoPrimary("Du wurdest getötet und respawnst in ")
            geckoHighlight(settings.seekerRespawnTimeSeconds.toString())
            geckoPrimary(" Sekunden als ")
            append(GeckoGameRole.SEEKER.displayText)
            geckoPrimary(".")
        }

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

                player.sendActionBar(buildText {
                    geckoPrimary("Respawn in ")
                    geckoHighlight(secondsLeft.toString())
                    geckoPrimary(" Sekunden")
                })

                return@forEach
            }

            gamePlayer.respawnSecondsLeft = null
            gamePlayer.respawnAsSeeker(settings.map)

            player.sendText {
                appendPrefix()
                geckoPrimary("Du bist wieder im Spiel.")
            }
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

            sendText {
                appendPrefix()
                text(player.username, gamePlayer.role.color, TextDecoration.BOLD)
                geckoPrimary(" hat das Spiel verlassen.")
            }

            statsTracker.markLeft(gamePlayer.playerUuid)
            gamePlayers.removeAll { it.playerUuid == gamePlayer.playerUuid }
            gamePlayer.clearRespawnState()
            gamePlayer.resetSpeed()
            gameInfoBossBar.removeViewer(player)

            if (gamePlayer.role != GeckoGameRole.SPECTATOR) {
                GeckoGamePunisher.punish(player, "Verlassen des Spiels während der Runde")
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
                it.player.sendText {
                    appendNewline()
                    appendPrefix()
                    text(
                        "Der Sucher hat das Spiel verlassen.",
                        GeckoGameRole.SEEKER.color,
                        TextDecoration.BOLD
                    )
                    appendNewline()
                    appendPrefix()
                    geckoPrimary("Du wurdest zufällig als neuer ")
                    append(GeckoGameRole.SEEKER.displayText)
                    geckoPrimary(" ausgewählt.")
                }
            } else {
                it.player.sendText {
                    appendNewline()
                    appendPrefix()
                    text(
                        "Der Sucher hat das Spiel verlassen.",
                        GeckoGameRole.SEEKER.color,
                        TextDecoration.BOLD
                    )
                    appendNewline()
                    appendPrefix()
                    geckoHighlight(newSeeker.player.username)
                    geckoPrimary(" ist nun ")
                    append(GeckoGameRole.SEEKER.displayText)
                }
            }
        }
    }

    private suspend fun tryStart() {
        val countdown = countdownSeconds ?: return

        if (countdown <= 0 && state == GeckoGameState.LOBBY) {
            state = GeckoGameState.HIDING
            geckoAsyncScope.launch {
                phaseGame()
            }
        }
    }

    suspend fun phaseGame() {
        if (lobbyCountdownJob != null) {
            lobbyCountdownJob?.cancel()
            lobbyCountdownJob = null
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

        forEachPlayer { ScreenFade.play(it, 10, 20, 15) }
        delay(10.ticks)

        coroutineScope {
            gamePlayers.map { player ->
                async {
                    player.applyGameMode()
                    player.applySpeed()
                    player.applyEquipment()
                    player.updateSocialGroup()
                    player.teleportToSpawn(settings.map)
                    player.player.hideBossBar(bossBar)
                    player.player.playSound(GeckoSounds.PHASE_GAME, Sound.Emitter.self())
                }
            }.awaitAll()
        }

        geckoAsyncScope.launch {
            delay(35.ticks)
            forEachGamePlayer { if (it.playerOrNull != null) it.sendRoleMessage() }
        }

        gameTimerSeconds = settings.roundTimeSeconds
        gameTimerJob = geckoAsyncScope.runAtFixedRate(1.seconds) {
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
    }

    private lateinit var gameInfoBarJob: Job
    private fun startGameInfoBar() {
        players.forEach {
            gameInfoBossBar.addViewer(it)
        }

        gameInfoBarJob = geckoAsyncScope.runAtFixedRate(500.milliseconds) {
            gameInfoBossBar.name(buildText {
                geckoHighlight(settings.map.mapDisplayName)

                val nextBeam = periodicBeamManager.nextBeam ?: return@buildText

                appendSpace()
                append(
                    Component.`object`(
                        ObjectContents.sprite(
                            key("minecraft", "gui"),
                            key("minecraft", "mob_effect/glowing")
                        )
                    )
                )
                appendSpace()
                append(
                    CommonComponents.formatTime(
                        Duration.between(OffsetDateTime.now(), nextBeam)
                            .coerceAtLeast(Duration.ZERO)
                            .toKotlinDuration(),
                        showSeconds = true,
                        shortForms = false,
                        timeColor = Colors.WHITE
                    )
                )
            })
        }
    }

    fun stopGameInfoBar() {
        if (this::gameInfoBarJob.isInitialized) {
            gameInfoBarJob.cancel()
        }

        forEachPlayer { gameInfoBossBar.removeViewer(it) }
    }

    private var waitingBossBarIndex = 0

    private fun updateCountdownBossBar() {
        val text = if (countdownSeconds != null) {
            countDownBossBar(countdownSeconds!!)
        } else {
            val text = if (waitingBossBarIndex == 0) {
                countdownBossBar2
            } else {
                countdownBossBar3
            }

            waitingBossBarIndex = (waitingBossBarIndex + 1) % 2
            text
        }

        bossBar.name(text)
        players.forEach { player ->
            player.showBossBar(bossBar)
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
            broadcastCountdown(secondsUntilSearch, SEARCH_COUNTDOWN_SUBTITLE)
        }

        if (state == GeckoGameState.HIDING && currentTimer <= searchStartAt) {
            state = GeckoGameState.SEARCHING

            gamePlayers.filter { it.role == GeckoGameRole.SEEKER }.forEach {
                it.player.teleport(settings.map.mapLocations.spawn)
            }

            sendText {
                appendPrefix()
                geckoPrimary("Die Suche beginnt.")
            }

            forEachPlayer {
                it.showTitle {
                    title {
                        text("Die Suche beginnt", GeckoGameRole.SEEKER.color, TextDecoration.BOLD)
                    }
                    subtitle = SEARCH_START_SUBTITLE
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
            broadcastCountdown(currentTimer, END_COUNTDOWN_SUBTITLE)
        }

        if (currentTimer <= 0) {
            beginEnding(GeckoGameEndReason.HIDER_WIN)
        }
    }

    private fun broadcastCountdown(secondsLeft: Int, subtitleText: Component) = forEachPlayer { player ->
        CountdownTitle.show(player, secondsLeft.toString(), subtitleText)
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

    fun sendText(builder: SurfComponentBuilder.() -> Unit) = forEachPlayer { it.sendText(builder) }

    val gameInfo
        get() = GeckoGameInfo(
            internalId,
            playerCount,
            settings.maxPlayers,
            settings.map.mapDisplayName
        )

    private companion object {
        val SEARCH_COUNTDOWN_SUBTITLE = buildText {
            geckoUseless("bis die Sucher losgelassen werden".toSmallCaps())
        }

        val SEARCH_START_SUBTITLE = buildText {
            geckoUseless("Versteckt euch gut".toSmallCaps())
        }

        val END_COUNTDOWN_SUBTITLE = buildText {
            geckoUseless("bis das Spiel endet".toSmallCaps())
        }
    }
}
