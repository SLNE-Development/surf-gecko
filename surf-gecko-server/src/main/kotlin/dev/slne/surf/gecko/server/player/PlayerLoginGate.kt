package dev.slne.surf.gecko.server.player

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import dev.slne.surf.gecko.server.i18n.translate
import net.minestom.server.event.EventDispatcher

class PlayerLoginGate(private val playerLimit: PlayerLimit) {

    fun admit(player: LobbyPlayer): Boolean {
        val event = PlayerLoginEvent(player, initialResult(), player.translate("login.server-full"))
        EventDispatcher.call(event)

        if (event.isAllowed) return true

        player.kick(event.kickMessage)
        return false
    }

    private fun initialResult() = when {
        playerLimit.playerCount > playerLimit.maxPlayers -> PlayerLoginEvent.Result.KICK_FULL
        else -> PlayerLoginEvent.Result.ALLOWED
    }
}
