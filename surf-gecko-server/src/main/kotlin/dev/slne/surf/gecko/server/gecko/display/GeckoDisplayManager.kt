package dev.slne.surf.gecko.server.gecko.display

import dev.slne.surf.api.core.messages.adventure.bossBar
import net.kyori.adventure.bossbar.BossBar
import net.minestom.server.entity.Player

object GeckoDisplayManager {
    private val placeholderBossBar = bossBar {
        color = BossBar.Color.PINK
    }


    fun showBossBar(player: Player) = player.showBossBar(placeholderBossBar)
}