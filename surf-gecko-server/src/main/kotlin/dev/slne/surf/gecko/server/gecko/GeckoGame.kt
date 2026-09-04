package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.*
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.game.GeckoPlayerRoleSelector
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import kotlinx.coroutines.*
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
    }

    var gameTimerSeconds: Int? = null
    private var gameTimerJob: Job? = null

    val lobbyPlayers = mutableSetOf<GeckoLobbyPlayer>()
    val gamePlayers = mutableSetOf<GeckoGamePlayer>()

    val countdownBossBar2 = buildText {
        primary("Warte auf weitere Spieler.. ")
    }

    val countdownBossBar3 = buildText {
        primary("Warte auf weitere Spieler...")
    }

    private val bossBar = bossBar {
        name {
            append(countdownBossBar3)
        }
    }

    private val placeholderBossBar = bossBar {}

    fun countDownBossBar(seconds: Int) = buildText {
        primary("Spiel startet in ")
        variableValue(seconds)
        primary(" Sekunden.")
    }

    fun backToLobbyBossBar(seconds: Int) = buildText {
        primary("Zurück zur Lobby in ")
        variableValue(seconds)
        primary(" Sekunden...")
    }

    val playerCount get() = lobbyPlayers.size + gamePlayers.size
    val freeSlots get() = settings.maxPlayers - playerCount
    val joinable get() = state.acceptsPlayers() && freeSlots > 0

    val players
        get() = lobbyPlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        } + gamePlayers.map {
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it.playerUuid)
        }

    fun findGamePlayer(playerUuid: UUID) = gamePlayers.firstOrNull { it.playerUuid == playerUuid }

    fun isTeamDamage(attacker: GeckoGamePlayer, victim: GeckoGamePlayer) =
        attacker.role == victim.role

    private var endingJob: Job? = null
    private var endingTimerSeconds: Int? = null

    fun beginEnding(reason: GeckoGameEndReason) {
        state = GeckoGameState.ENDING
        gameTimerJob?.cancel()
        gameTimerJob = null
        endingTimerSeconds = 30

        sendText {
            appendInfoPrefix()
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
            appendInfoPrefix()
            info("Du wirst in ".toSmallCaps())
            variableValue(endingTimerSeconds ?: 30)
            info(" Sekunden in eine neue Runde geschickt.".toSmallCaps())
            appendNewline()
            appendInfoPrefix()
            info("Map ")
            variableValue(settings.map.mapDisplayName)
            info(" von ")
            appendNewline()
            appendInfoPrefix()
            info(settings.map.mapAuthors.map { it.name }.joinToString { "," })
        }

        endingJob = geckoAsyncScope.runAtFixedRate(1.seconds, 1.seconds) {
            val currentEndingSeconds = endingTimerSeconds ?: return@runAtFixedRate
            endingTimerSeconds = currentEndingSeconds - 1

            bossBar.name(countDownBossBar(currentEndingSeconds))

            forEachPlayer {
                it.showBossBar(placeholderBossBar)
                it.showBossBar(bossBar)
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
            appendInfoPrefix()
            info("Du wurdest gefunden und bist nun ")
            append(GeckoGameRole.SPECTATOR.displayText)
            info(".")
        }
    }

    private fun startSeekerRespawn(gamePlayer: GeckoGamePlayer) {
        gamePlayer.respawnSecondsLeft = settings.seekerRespawnTimeSeconds
        gamePlayer.moveToSeekerLobby(settings.map)

        gamePlayer.player.sendText {
            appendInfoPrefix()
            info("Du wurdest getötet und respawnst in ")
            variableValue(settings.seekerRespawnTimeSeconds)
            info(" Sekunden als ")
            append(GeckoGameRole.SEEKER.displayText)
            info(".")
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
                    info("Respawn in ")
                    variableValue(secondsLeft)
                    info(" Sekunden")
                })

                return@forEach
            }

            gamePlayer.respawnSecondsLeft = null
            gamePlayer.respawnAsSeeker(settings.map)

            player.sendText {
                appendSuccessPrefix()
                success("Du bist wieder im Spiel.")
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

        updateCountdownBossBar()
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

        tickRespawns()
        checkForGameEnd()

        if (state == GeckoGameState.HIDING && currentTimer <= (settings.roundTimeSeconds - 45)) {
            state = GeckoGameState.SEARCHING

            gamePlayers.filter { it.role == GeckoGameRole.SEEKER }.forEach {
                it.player.teleport(settings.map.mapLocations.spawn)
            }

            sendText {
                appendInfoPrefix()
                spacer("Die Suche beginnt.")
            }

            gamePlayers.forEach {
                it.player.playSound(true) {
                    type(key("item.goat_horn.sound.1"))
                    pitch(1.3f)
                }
            }
        }

        if (currentTimer <= 0) {
            beginEnding(GeckoGameEndReason.HIDER_WIN)
        }
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
}
