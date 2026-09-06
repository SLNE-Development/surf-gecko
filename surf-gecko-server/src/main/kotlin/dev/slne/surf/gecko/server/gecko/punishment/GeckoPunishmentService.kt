package dev.slne.surf.gecko.server.gecko.punishment

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoPunishmentRepository
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.util.GECKO_SECONDARY
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

object GeckoPunishmentService {
    private val punishments = ConcurrentHashMap<UUID, GeckoGamePunishment>()
    private val bossBars = ConcurrentHashMap<UUID, BossBar>()

    private lateinit var job: Job

    fun init() {
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            update()
        }
    }

    fun shutdown() {
        if (::job.isInitialized && job.isActive) {
            job.cancel()
        }

        MinecraftServer.getConnectionManager().onlinePlayers.forEach { hideBossBar(it) }

        punishments.clear()
        bossBars.clear()
    }

    fun apply(player: Player, punishment: GeckoGamePunishment) {
        punishments[player.uuid] = punishment

        player.sendText {
            appendNewline()
            appendPrefix()
            error("Du wurdest gesperrt.", TextDecoration.BOLD)
            appendNewline()
            appendPrefix()
            geckoPrimary("Grund: ")
            geckoSecondary(punishment.reason)
            appendNewline()
            appendPrefix()
            appendRemaining(punishment)
            appendNewline()
        }
    }

    fun activePunishment(playerUuid: UUID) = punishments[playerUuid]?.takeIf { it.isActive() }

    fun release(playerUuid: UUID) {
        punishments.remove(playerUuid)
        bossBars.remove(playerUuid)
    }

    suspend fun refresh(playerUuid: UUID): GeckoGamePunishment? {
        val punishment = withContext(Dispatchers.IO) {
            GeckoPunishmentRepository.fetchActivePunishment(playerUuid)
        }

        if (punishment == null || !punishment.isActive()) {
            punishments.remove(playerUuid)
            return null
        }

        punishments[playerUuid] = punishment
        return punishment
    }

    suspend fun handleJoin(player: Player) {
        val punishment = refresh(player.uuid) ?: return
        sendNo(player, punishment)
    }

    suspend fun preventJoin(player: Player): Boolean {
        val punishment = refresh(player.uuid) ?: return false
        sendNo(player, punishment)
        return true
    }

    private fun sendNo(player: Player, punishment: GeckoGamePunishment) {
        player.sendText {
            appendNewline()
            appendPrefix()
            error("Du kannst aktuell keinem Spiel beitreten.", TextDecoration.BOLD)
            appendNewline()
            appendPrefix()
            geckoPrimary("Grund: ")
            geckoSecondary(punishment.reason)
            appendNewline()
            appendPrefix()
            appendRemaining(punishment)
            appendNewline()
        }
    }

    private fun update() {
        for (player in MinecraftServer.getConnectionManager().onlinePlayers) {
            val punishment = punishments[player.uuid]

            if (punishment != null && !punishment.isActive()) {
                punishments.remove(player.uuid)
            }

            val active = activePunishment(player.uuid)

            if (active == null || !GeckoLobby.contains(player)) {
                hideBossBar(player)
                continue
            }

            showBossBar(player, active)
        }
    }

    private fun showBossBar(player: Player, punishment: GeckoGamePunishment) {
        val bossBar = bossBars.computeIfAbsent(player.uuid) {
            bossBar {
                color = BossBar.Color.PINK
            }
        }

        bossBar.name(buildText {
            error("Gesperrt", TextDecoration.BOLD)
            spacer(" » ")
            appendRemaining(punishment)
        })

        player.showBossBar(bossBar)
    }

    private fun hideBossBar(player: Player) {
        val bossBar = bossBars.remove(player.uuid) ?: return
        player.hideBossBar(bossBar)
    }
}

private fun SurfComponentBuilder.appendRemaining(punishment: GeckoGamePunishment) {
    val remaining = punishment.remaining()

    if (remaining == null) {
        geckoPrimary("Dauer: ")
        geckoSecondary("Permanent")
        return
    }

    geckoPrimary("Noch ")
    appendTime(remaining, showSeconds = true, shortForms = true, timeColor = GECKO_SECONDARY)
}
