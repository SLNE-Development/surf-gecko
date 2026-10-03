package dev.slne.surf.gecko.server.performance.monitor

import dev.slne.surf.api.core.messages.adventure.bossBar
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import dev.slne.surf.gecko.server.i18n.language
import kotlinx.coroutines.Job
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*
import kotlin.time.Duration.Companion.seconds

object StatusBarManager {
    private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0

    private val statusBossBars = EnumMap<GeckoLanguage, BossBar>(GeckoLanguage::class.java).apply {
        for (language in GeckoLanguage.entries) {
            put(language, bossBar {
                color = BossBar.Color.GREEN
                overlay = BossBar.Overlay.NOTCHED_20
                progress = 1f
            })
        }
    }

    private var job: Job? = null

    fun init() {
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            SystemStatistics.sample()
            update()
        }

        PlayerLanguages.onChange { player ->
            if (isVisible(player)) {
                hide(player)
                player.showBossBar(statusBossBars.getValue(player.language))
            }
        }
    }

    fun shutdown() {
        job?.cancel()
        job = null

        statusBossBars.values.forEach { MinecraftServer.getBossBarManager().destroyBossBar(it) }
    }

    fun toggle(player: Player): Boolean {
        if (isVisible(player)) {
            hide(player)
            return false
        }

        player.showBossBar(statusBossBars.getValue(player.language))
        update()

        return true
    }

    fun isVisible(player: Player): Boolean {
        val shown = MinecraftServer.getBossBarManager().getPlayerBossBars(player)
        return statusBossBars.values.any { it in shown }
    }

    private fun hide(player: Player) = statusBossBars.values.forEach { player.hideBossBar(it) }

    private fun update() {
        val bossBarManager = MinecraftServer.getBossBarManager()
        if (statusBossBars.values.all { bossBarManager.getBossBarViewers(it).isEmpty() }) {
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

        val progress = (tps / TickStatistics.targetTps).toFloat().coerceIn(0f, 1f)
        val color = worstOf(tpsGrade, msptGrade, memoryGrade, cpuGrade).barColor

        for ((language, bossBar) in statusBossBars) {
            bossBar.name(
                GeckoTranslations.render(
                    language,
                    "statusbar.format",
                    "tps" to text(decimal(tps), tpsGrade.textColor),
                    "mspt" to text(decimal(mspt), msptGrade.textColor),
                    "ram_used" to text(memory(usedMemory), memoryGrade.textColor),
                    "ram_max" to text(memory(maxMemory), memoryGrade.textColor),
                    "cpu" to text(percent(cpuLoad), cpuGrade.textColor)
                )
            )
            bossBar.progress(progress)
            bossBar.color(color)
        }
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
