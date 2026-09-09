package dev.slne.surf.gecko.server.anticheat.report

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.anticheat.AntiCheatSnapshot
import dev.slne.surf.gecko.server.anticheat.AntiCheatTracker
import dev.slne.surf.gecko.server.anticheat.check.Violation
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import kotlinx.coroutines.Job
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private val UPDATE_INTERVAL = 50.milliseconds
private const val ALERT_DURATION_MILLIS = 2_500L

object AntiCheatDisplay {

    private val alertViewers = ConcurrentHashMap.newKeySet<UUID>()
    private val debugTargets = ConcurrentHashMap<UUID, UUID>()
    private val activeAlerts = ConcurrentHashMap<UUID, ActiveAlert>()

    private var job: Job? = null

    fun init() {
        if (job != null) {
            return
        }

        job = geckoAsyncScope.runAtFixedRate(UPDATE_INTERVAL, taskName = "anticheat-display") {
            render()
        }

        MinecraftServer.LOGGER.info(
            "[Anticheat] Actionbar Anzeige gestartet, sichtbar über /anticheat alerts und /anticheat debug."
        )
    }

    fun shutdown() {
        job?.cancel()
        job = null

        alertViewers.clear()
        debugTargets.clear()
        activeAlerts.clear()
    }

    fun forget(player: Player) {
        alertViewers.remove(player.uuid)
        debugTargets.remove(player.uuid)
        activeAlerts.remove(player.uuid)
        debugTargets.values.removeIf { it == player.uuid }
    }

    fun toggleAlerts(player: Player): Boolean {
        if (!alertViewers.add(player.uuid)) {
            alertViewers.remove(player.uuid)
            return false
        }

        return true
    }

    fun watch(viewer: Player, target: Player) {
        debugTargets[viewer.uuid] = target.uuid
    }

    fun stopWatching(viewer: Player) = debugTargets.remove(viewer.uuid) != null

    fun report(player: Player, violation: Violation) {
        if (violation.notable) {
            MinecraftServer.LOGGER.info(
                "[Anticheat] {} failed {} x{} ({})",
                player.username,
                violation.type.displayName,
                violation.count,
                violation.detail,
            )
        }

        if (alertViewers.isEmpty() && debugTargets.isEmpty()) {
            return
        }

        val alert = ActiveAlert(
            username = player.username,
            violation = violation,
            expiresAt = System.currentTimeMillis() + ALERT_DURATION_MILLIS,
        )

        for (viewer in alertViewers) {
            activeAlerts[viewer] = alert
        }

        for ((viewer, target) in debugTargets) {
            if (target == player.uuid) {
                activeAlerts[viewer] = alert
            }
        }
    }

    private fun render() {
        if (alertViewers.isEmpty() && debugTargets.isEmpty()) {
            return
        }

        val connectionManager = MinecraftServer.getConnectionManager()
        val now = System.currentTimeMillis()
        val viewers = HashSet<UUID>(alertViewers)

        viewers.addAll(debugTargets.keys)

        for (uuid in viewers) {
            val viewer = connectionManager.getOnlinePlayerByUuid(uuid)

            if (viewer == null) {
                alertViewers.remove(uuid)
                debugTargets.remove(uuid)
                activeAlerts.remove(uuid)
                continue
            }

            val alert = activeAlerts[uuid]

            if (alert != null && alert.expiresAt > now) {
                viewer.sendActionBar(buildText { renderAlert(alert) })
                continue
            }

            activeAlerts.remove(uuid)

            val targetUuid = debugTargets[uuid] ?: continue
            val target = connectionManager.getOnlinePlayerByUuid(targetUuid)

            if (target == null) {
                debugTargets.remove(uuid)
                continue
            }

            val snapshot = AntiCheatTracker.find(target)?.snapshot ?: AntiCheatSnapshot.EMPTY

            viewer.sendActionBar(buildText { renderDebug(target, snapshot) })
        }
    }

    private fun SurfComponentBuilder.renderAlert(alert: ActiveAlert) {
        variableValue(alert.username)
        appendSpace()
        geckoPrimary("failed")
        appendSpace()
        error(alert.violation.type.displayName)
        appendSpace()
        geckoHighlight("x${alert.violation.count}")

        if (alert.violation.detail.isEmpty()) {
            return
        }

        appendSpace()
        spacer("(")
        geckoSecondary(alert.violation.detail)
        spacer(")")
    }

    private fun SurfComponentBuilder.renderDebug(target: Player, snapshot: AntiCheatSnapshot) {
        geckoHighlight("AC")
        separator()
        variableValue(target.username)
        separator()

        val exemption = snapshot.exemption

        if (exemption != null) {
            geckoPrimary("ausgesetzt")
            spacer(": ")
            geckoSecondary(exemption.displayName)
        } else {
            geckoPrimary("O ")
            offset(snapshot.offset)
            separator()
            geckoPrimary("H ")
            geckoSecondary(decimal(snapshot.horizontalExcess))
            appendSpace()
            geckoPrimary("V ")
            geckoSecondary(decimal(snapshot.verticalExcess))
            separator()
            geckoPrimary("S ")
            geckoSecondary(decimal(snapshot.speed))
            separator()
            geckoPrimary("Boden ")
            ground(snapshot.claimedGround, snapshot.simulatedGround)
        }

        separator()
        geckoPrimary("VL ")

        if (snapshot.violationLevel > 0.0) {
            warning("%.1f".format(Locale.ROOT, snapshot.violationLevel))
        } else {
            success("0.0")
        }
    }

    private fun SurfComponentBuilder.offset(value: Double) {
        val text = decimal(value)

        when {
            value > 0.1 -> error(text)
            value > 0.01 -> warning(text)
            else -> success(text)
        }
    }

    private fun SurfComponentBuilder.ground(claimed: Boolean, simulated: Boolean) {
        val text = if (claimed) "ja" else "nein"

        if (claimed == simulated) {
            success(text)
        } else {
            error(text)
        }
    }

    private fun SurfComponentBuilder.separator() {
        appendSpace()
        darkSpacer("|")
        appendSpace()
    }

    private fun decimal(value: Double) = "%.5f".format(Locale.ROOT, value)

    private data class ActiveAlert(
        val username: String,
        val violation: Violation,
        val expiresAt: Long,
    )
}
