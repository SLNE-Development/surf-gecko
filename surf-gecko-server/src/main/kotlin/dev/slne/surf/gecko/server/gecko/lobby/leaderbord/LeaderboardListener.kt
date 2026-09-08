package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoPlayerNameRepository
import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import kotlinx.coroutines.launch
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent

object LeaderboardListener : MinestomListener {
    @EventHandler
    fun onSpawn(event: PlayerSpawnEvent) {
        val player = event.player

        geckoAsyncScope.launch {
            if (event.isFirstSpawn) {
                GeckoPlayerNameRepository.saveName(player.uuid, player.username)
            }

            LeaderboardManager.show(player)
        }
    }

    @EventHandler
    fun onDisconnect(event: PlayerDisconnectEvent) {
        LeaderboardManager.invalidate(event.player.uuid)
    }
}
