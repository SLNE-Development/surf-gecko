package dev.slne.surf.gecko.server.gecko.social.chat

import dev.slne.minestom.lobby.api.chat.AsyncChatEvent
import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

object SocialChatListener : MinestomListener {
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player

        event.viewers.removeIf { it is Player }
        event.viewers.addAll(MinecraftServer.getConnectionManager().onlinePlayers.filter { viewer ->
            SocialGroupManager.canSee(
                player.uuid,
                viewer.uuid
            )
        })
    }
}