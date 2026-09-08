package dev.slne.surf.gecko.server.performance.tps

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import kotlinx.coroutines.Job
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

object TpsBarManager {
    private val UPDATE_INTERVAL = 1.seconds

    private val tpsBossBar = bossBar {
        color = BossBar.Color.GREEN
        overlay = BossBar.Overlay.NOTCHED_20
        progress = 1f
    }

    private var job: Job? = null

    fun init() {
        job = geckoAsyncScope.runAtFixedRate(UPDATE_INTERVAL) { update() }
    }

    fun shutdown() {
        job?.cancel()
        job = null

        MinecraftServer.getBossBarManager().destroyBossBar(tpsBossBar)
    }

    fun toggle(player: Player): Boolean {
        if (isVisible(player)) {
            player.hideBossBar(tpsBossBar)
            return false
        }

        player.showBossBar(tpsBossBar)
        update()

        return true
    }

    fun isVisible(player: Player) =
        tpsBossBar in MinecraftServer.getBossBarManager().getPlayerBossBars(player)

    private fun update() {
        if (MinecraftServer.getBossBarManager().getBossBarViewers(tpsBossBar).isEmpty()) {
            return
        }

        val tps = TickStatistics.tps
        val mspt = TickStatistics.mspt

        tpsBossBar.name(buildText {
            geckoPrimary("TPS")
            spacer(": ")
            text(format(tps), tpsColor(tps))
            appendSpace()
            darkSpacer("|")
            appendSpace()
            geckoPrimary("MSPT")
            spacer(": ")
            text(format(mspt), msptColor(mspt))
        })

        tpsBossBar.progress((tps / TickStatistics.targetTps).toFloat().coerceIn(0f, 1f))
        tpsBossBar.color(barColor(tps))
    }

    private fun format(value: Double) = "%.2f".format(Locale.ROOT, value)

    private fun tpsColor(tps: Double): TextColor = when {
        tps >= TickStatistics.targetTps * 0.95 -> NamedTextColor.GREEN
        tps >= TickStatistics.targetTps * 0.75 -> NamedTextColor.YELLOW
        else -> NamedTextColor.RED
    }

    private fun msptColor(mspt: Double): TextColor = when {
        mspt <= TickStatistics.targetMspt * 0.5 -> NamedTextColor.GREEN
        mspt <= TickStatistics.targetMspt -> NamedTextColor.YELLOW
        else -> NamedTextColor.RED
    }

    private fun barColor(tps: Double) = when {
        tps >= TickStatistics.targetTps * 0.95 -> BossBar.Color.GREEN
        tps >= TickStatistics.targetTps * 0.75 -> BossBar.Color.YELLOW
        else -> BossBar.Color.RED
    }
}
