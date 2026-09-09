package dev.slne.surf.gecko.server.anticheat.check

import dev.slne.surf.gecko.server.config.Config
import java.util.EnumMap

class ViolationTracker(private val settings: Config.AntiCheatConfig) {

    private val levels = EnumMap<CheckType, Double>(CheckType::class.java)
    private val counts = EnumMap<CheckType, Int>(CheckType::class.java)
    private val lastLogs = EnumMap<CheckType, Long>(CheckType::class.java)

    fun flag(type: CheckType, detail: String): Violation {
        val level = levels.merge(type, 1.0, Double::plus) ?: 1.0
        val count = counts.merge(type, 1, Int::plus) ?: 1

        return Violation(type, detail, level, count, notable(type, level))
    }

    fun decay() {
        if (levels.isEmpty()) {
            return
        }

        val iterator = levels.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val decayed = entry.value - settings.violationDecay

            if (decayed <= 0.0) {
                iterator.remove()
            } else {
                entry.setValue(decayed)
            }
        }
    }

    fun clearMitigable() {
        levels.keys.removeIf { it.mitigable }
    }

    fun total() = levels.values.sum()

    fun levels(): Map<CheckType, Double> = EnumMap(levels)

    fun counts(): Map<CheckType, Int> = EnumMap(counts)

    fun reset() {
        levels.clear()
        counts.clear()
        lastLogs.clear()
    }

    private fun notable(type: CheckType, level: Double): Boolean {
        if (level < settings.alertThreshold) {
            return false
        }

        val now = System.currentTimeMillis()

        if (now - (lastLogs[type] ?: 0L) < settings.alertCooldownMillis) {
            return false
        }

        lastLogs[type] = now

        return true
    }
}
