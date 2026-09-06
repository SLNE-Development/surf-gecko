package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.heartbeat.GeckoHeartbeat
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbSpawner
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.game.GeckoPlayerRoleSelector
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.punishment.GeckoGamePunisher
import dev.slne.surf.gecko.server.gecko.scoreboard.GeckoScoreboardManager
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.gecko.util.*
import kotlinx.coroutines.*
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.util.*
import kotlin.time.Duration.Companion.seconds

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

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()

    val countdownBossBar2 = buildText {
        geckoPrimary("Warte auf weitere Spieler.. ")
    }

    val countdownBossBar3 = buildText {
        geckoPrimary("Warte auf weitere Spieler...")
    }

    private val bossBar = bossBar {
        name {
            append(countdownBossBar3)
        }
    }

    private val placeholderBossBar = bossBar {}

    fun countDownBossBar(seconds: Int) = buildText {
        geckoPrimary("Spiel startet in ")
        geckoSecondary(seconds.toString())
        geckoPrimary(" Sekunden.")
    }

    fun backToLobbyBossBar(seconds: Int) = buildText {
        geckoPrimary("Zurück zur Lobby in ")
        geckoSecondary(seconds.toString())
        geckoPrimary(" Sekunden...")
    }

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = (lobbyPlayers.map { it.playerUuid } + gamePlayers.map { it.playerUuid })
            .distinct()
            .map { MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it) }

    fun findGamePlayer(playerUuid: UUID) = gamePlayers.firstOrNull { it.playerUuid == playerUuid }

    fun stopHeartbeat() = heartbeat.stop()
    fun stopOrbSpawner() = orbSpawner.stop()

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
        endingTimerSeconds = 10

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
            geckoPrimary("Du wirst in ".toSmallCaps())
            geckoHighlight((endingTimerSeconds ?: 30).toString())
            geckoPrimary(" Sekunden in")
            appendNewline()
            appendPrefix()
            geckoPrimary("die Lobby geschickt.")
        }

        endingJob = geckoAsyncScope.runAtFixedRate(1.seconds, 1.seconds) {
            val currentEndingSeconds = endingTimerSeconds ?: return@runAtFixedRate
            endingTimerSeconds = currentEndingSeconds - 1

            GeckoScoreboardManager.updateSidebar(this@GeckoGame)
            bossBar.name(backToLobbyBossBar(currentEndingSeconds))

            if (currentEndingSeconds <= 0) {
                forEachPlayer {
                    it.hideBossBar(placeholderBossBar)
                    it.hideBossBar(bossBar)
                }
            } else {
                forEachPlayer {
                    it.showBossBar(placeholderBossBar)
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

        when (gamePlayer.role) {
            GeckoGameRole.SEEKER -> startSeekerRespawn(gamePlayer)
            GeckoGameRole.HIDER -> handleHiderDeath(gamePlayer)
            GeckoGameRole.SPECTATOR -> Unit
        }
    }

    private fun handleHiderDeath(gamePlayer: GeckoGamePlayer) {
        if (settings.respawnHidersAsSeekers) {
            gamePlayer.role = GeckoGameRole.SEEKER
            startSeekerRespawn(gamePlayer)
            return
        }

        gamePlayer.role = GeckoGameRole.SPECTATOR
        gamePlayer.applyGameMode()
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

            when (gamePlayer.role) {
                GeckoGameRole.SEEKER -> {
                    val seekerCount = gamePlayers.count { it.role == GeckoGameRole.SEEKER }

                    if (seekerCount <= 1) {
                        choseNewRandomSeeker()
                    }
                }

                GeckoGameRole.HIDER -> {
                    if (gamePlayers.count { it.role == GeckoGameRole.HIDER } <= 0) {
                        beginEnding(GeckoGameEndReason.SEEKER_WIN)
                    }
                }

                GeckoGameRole.SPECTATOR -> return
            }

            gamePlayers.removeAll { it.playerUuid == gamePlayer.playerUuid }
            gamePlayer.clearRespawnState()

            GeckoGamePunisher.punish(player)
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

        val newSeeker = hiders.random()
        newSeeker.role = GeckoGameRole.SEEKER
        newSeeker.applyGameMode()
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

        coroutineScope {
            gamePlayers.map { player ->
                async {
                    player.applyGameMode()
                    player.applyEquipment()
                    player.teleportToSpawn(settings.map)
                    player.player.hideBossBar(bossBar)
                    player.player.hideBossBar(placeholderBossBar)
                }
            }.awaitAll()
        }

        gameTimerSeconds = settings.roundTimeSeconds
        gameTimerJob = geckoAsyncScope.runAtFixedRate(1.seconds) {
            tickGame()
        }
        heartbeat.start()
        orbSpawner.start()
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
        players.filterNotNull().forEach { player ->
            player.showBossBar(placeholderBossBar)
            player.showBossBar(bossBar)
        }
    }

    private suspend fun tickGame() {
        val currentTimer = gameTimerSeconds ?: return

        gameTimerSeconds = currentTimer - 1

        GeckoScoreboardManager.updateSidebar(this@GeckoGame)
        tickRespawns()
        checkForGameEnd()

        val searchStartAt = settings.roundTimeSeconds - settings.hidingTimeSeconds
        val secondsUntilSearch = currentTimer - searchStartAt

        if (state == GeckoGameState.HIDING && secondsUntilSearch in 1..GeckoSounds.COUNTDOWN_SECONDS) {
            broadcastCountdown(
                secondsUntilSearch,
                GeckoGameRole.SEEKER.color,
                SEARCH_COUNTDOWN_SUBTITLE
            )
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
            broadcastCountdown(currentTimer, Colors.ERROR, END_COUNTDOWN_SUBTITLE)
        }

        if (currentTimer <= 0) {
            beginEnding(GeckoGameEndReason.HIDER_WIN)
        }
    }

    private fun broadcastCountdown(
        secondsLeft: Int,
        color: TextColor,
        subtitleText: Component
    ) = forEachPlayer { player ->
        player.showTitle {
            title {
                text(secondsLeft.toString(), color, TextDecoration.BOLD)
            }
            subtitle = subtitleText
            times {
                fadeIn(0)
                stay(18)
                fadeOut(4)
            }
        }

        player.playSound(GeckoSounds.countdownTick(secondsLeft), Sound.Emitter.self())
    }

    private suspend fun checkForGameEnd() {
        if (state != GeckoGameState.SEARCHING) {
            return
        }

        if (gamePlayers.count { it.role == GeckoGameRole.HIDER } <= 0) {
            beginEnding(GeckoGameEndReason.SEEKER_WIN)
        }
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
