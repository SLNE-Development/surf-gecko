package dev.slne.surf.gecko.server.performance.monitor

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
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

object StatusBarManager {
    private val UPDATE_INTERVAL = 1.seconds
    private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0

    private val statusBossBar = bossBar {
        color = BossBar.Color.GREEN
        overlay = BossBar.Overlay.NOTCHED_20
        progress = 1f
    }

    private var job: Job? = null

    fun init() {
        job = geckoAsyncScope.runAtFixedRate(UPDATE_INTERVAL) {
            SystemStatistics.sample()
            update()
        }
    }

    fun shutdown() {
        job?.cancel()
        job = null

        MinecraftServer.getBossBarManager().destroyBossBar(statusBossBar)
    }

    fun toggle(player: Player): Boolean {
        if (isVisible(player)) {
            player.hideBossBar(statusBossBar)
            return false
        }

        player.showBossBar(statusBossBar)
        update()

        return true
    }

    fun isVisible(player: Player) =
        statusBossBar in MinecraftServer.getBossBarManager().getPlayerBossBars(player)

    private fun update() {
        if (MinecraftServer.getBossBarManager().getBossBarViewers(statusBossBar).isEmpty()) {
            return
        }

        val tps = TickStatistics.tps
        val mspt = TickStatistics.mspt
        val usedMemory = SystemStatistics.usedMemory
        val maxMemory = SystemStatistics.maxMemory
        val memoryRatio = if (maxMemory > 0L) usedMemory.toDouble() / maxMemory else 0.0
        val cpuLoad = SystemStatistics.processCpuLoad

        val tpsGrade = Grade.atLeast(
            tps,
            TickStatistics.targetTps * 0.95,
            TickStatistics.targetTps * 0.75
        )
        val msptGrade = Grade.atMost(
            mspt,
            TickStatistics.targetMspt * 0.5,
            TickStatistics.targetMspt
        )
        val memoryGrade = Grade.atMost(memoryRatio, 0.6, 0.85)
        val cpuGrade = Grade.atMost(cpuLoad, 0.5, 0.8)

        statusBossBar.name(buildText {
            geckoPrimary("TPS")
            spacer(": ")
            text(decimal(tps), tpsGrade.textColor)

            separator()
            geckoPrimary("MSPT")
            spacer(": ")
            text(decimal(mspt), msptGrade.textColor)

            separator()
            geckoPrimary("RAM")
            spacer(": ")
            text(memory(usedMemory), memoryGrade.textColor)
            spacer("/")
            text(memory(maxMemory), memoryGrade.textColor)

            separator()
            geckoPrimary("CPU")
            spacer(": ")
            text(percent(cpuLoad), cpuGrade.textColor)
        })

        statusBossBar.progress((tps / TickStatistics.targetTps).toFloat().coerceIn(0f, 1f))
        statusBossBar.color(worstOf(tpsGrade, msptGrade, memoryGrade, cpuGrade).barColor)
    }

    private fun SurfComponentBuilder.separator() {
        appendSpace()
        darkSpacer("|")
        appendSpace()
    }

    private fun worstOf(vararg grades: Grade) = grades.max()

    private fun decimal(value: Double) = "%.2f".format(Locale.ROOT, value)

    private fun percent(load: Double) = "%.1f%%".format(Locale.ROOT, load * 100.0)

    private fun memory(bytes: Long): String {
        val megabytes = bytes / BYTES_PER_MEGABYTE

        return if (megabytes >= 1024.0) {
            "%.1f GB".format(Locale.ROOT, megabytes / 1024.0)
        } else {
            "%.0f MB".format(Locale.ROOT, megabytes)
        }
    }

    private enum class Grade(val textColor: TextColor, val barColor: BossBar.Color) {
        GOOD(NamedTextColor.GREEN, BossBar.Color.GREEN),
        WARNING(NamedTextColor.YELLOW, BossBar.Color.YELLOW),
        CRITICAL(NamedTextColor.RED, BossBar.Color.RED);

        companion object {
            fun atLeast(value: Double, good: Double, warning: Double) = when {
                value >= good -> GOOD
                value >= warning -> WARNING
                else -> CRITICAL
            }

            fun atMost(value: Double, good: Double, warning: Double) = when {
                value <= good -> GOOD
                value <= warning -> WARNING
                else -> CRITICAL
            }
        }
    }
}
