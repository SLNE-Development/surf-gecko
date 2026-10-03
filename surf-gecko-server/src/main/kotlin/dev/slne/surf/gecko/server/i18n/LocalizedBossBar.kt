package dev.slne.surf.gecko.server.i18n

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import java.util.EnumMap

class LocalizedBossBar(
    private var name: LocalizedComponent = LocalizedComponent { Component.empty() },
    private val color: BossBar.Color = BossBar.Color.PINK,
    private val overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS,
) {
    private val bars = EnumMap<GeckoLanguage, BossBar>(GeckoLanguage::class.java)

    @Synchronized
    private fun bar(language: GeckoLanguage): BossBar =
        bars.getOrPut(language) { BossBar.bossBar(name.render(language), 1f, color, overlay) }

    @Synchronized
    fun name(name: LocalizedComponent) {
        this.name = name
        for ((language, bar) in bars) {
            bar.name(name.render(language))
        }
    }

    fun show(player: Player) {
        val language = player.language
        for ((barLanguage, bar) in synchronized(this) { bars.toMap() }) {
            if (barLanguage != language) player.hideBossBar(bar)
        }
        player.showBossBar(bar(language))
    }

    fun hide(player: Player) {
        for (bar in synchronized(this) { bars.values.toList() }) {
            player.hideBossBar(bar)
        }
    }
}
