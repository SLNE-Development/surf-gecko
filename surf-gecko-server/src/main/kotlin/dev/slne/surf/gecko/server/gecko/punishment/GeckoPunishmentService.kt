package dev.slne.surf.gecko.server.gecko.punishment

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoPunishmentRepository
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.LocalizedComponent
import dev.slne.surf.gecko.server.i18n.formatDuration
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.i18n.translatable
import dev.slne.surf.gecko.server.i18n.translate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
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

        player.sendTranslated(
            "punishment.punished",
            "reason" to punishment.reasonText,
            "remaining" to punishment.remainingText
        )
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
        player.sendTranslated(
            "punishment.join-denied",
            "reason" to punishment.reasonText,
            "remaining" to punishment.remainingText
        )
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

        bossBar.name(player.translate("punishment.bossbar", "remaining" to punishment.remainingText))

        player.showBossBar(bossBar)
    }

    private fun hideBossBar(player: Player) {
        val bossBar = bossBars.remove(player.uuid) ?: return
        player.hideBossBar(bossBar)
    }
}

val GeckoGamePunishment.reasonText: LocalizedComponent
    get() = if (GeckoTranslations.has(reason)) translatable(reason) else LocalizedComponent { Component.text(reason) }

private val GeckoGamePunishment.remainingText: LocalizedComponent
    get() {
        val remaining = remaining() ?: return translatable("punishment.duration.permanent")
        return translatable("punishment.duration.remaining", "time" to formatDuration(remaining, short = true))
    }