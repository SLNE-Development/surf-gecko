package dev.slne.surf.gecko.server.gecko.lobby.elytra

import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerSwapItemEvent

object ElytraBoostListener : MinestomListener {
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player

        if (player.isOnGround) {
            ElytraBoostHandler.clearBoost(player)
        }
    }

    @EventHandler
    fun onSwap(event: PlayerSwapItemEvent) {
        if (event.player.gameMode == GameMode.CREATIVE || event.player.gameMode == GameMode.SPECTATOR) {
            return
        }

        if (!GeckoLobby.contains(event.player)) {
            return
        }

        ElytraBoostHandler.checkAndBoost(event.player)
        event.isCancelled = true
    }
}