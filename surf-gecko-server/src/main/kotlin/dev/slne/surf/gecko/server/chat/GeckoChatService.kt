package dev.slne.surf.gecko.server.chat

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.api.extension.PacketListenerManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.surf.gecko.server.chat.signature.RemoteChatSession
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import dev.slne.surf.gecko.server.player.GeckoPlayer
import dev.slne.surf.gecko.server.player.requireGeckoPlayer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.network.packet.client.play.ClientChatAckPacket
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket
import net.minestom.server.network.packet.client.play.ClientChatSessionUpdatePacket
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket
import net.minestom.server.utils.PacketSendingUtils

@Singleton
class GeckoChatService : GeckoService, EventRegistrar {

    override suspend fun start() {
        GeckoChatTypes.register()
        ChatTranslations.register()
        registerPacketListeners()
    }

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePlayerTick)
        node.addListener(::handlePlayerDisconnect)
    }

    private fun registerPacketListeners() = with(PacketListenerManager) {
        setPlayListener(ClientChatMessagePacket::class.java) { packet, player ->
            player.requireGeckoPlayer().chatHandler.handleChat(packet) { message ->
                ChatProcessor(player, message).process()
            }
        }

        setPlayListener(ClientChatAckPacket::class.java) { packet, player ->
            player.requireGeckoPlayer().chatHandler.handleChatAck(packet)
        }

        setPlayListener(ClientChatSessionUpdatePacket::class.java) { packet, player ->
            player.requireGeckoPlayer().chatHandler.handleChatSessionUpdate(packet) { session ->
                broadcastChatSession(player, session)
            }
        }

        setPlayListener(ClientCommandChatPacket::class.java) { packet, player ->
            player.requireGeckoPlayer().chatHandler.handleUnsignedCommandChat(packet.message()) { command ->
                CommandManager.execute(player, command)
            }
        }

        setPlayListener(ClientSignedCommandChatPacket::class.java) { packet, player ->
            player.requireGeckoPlayer().chatHandler.handleSignedCommandChat(packet) { command ->
                CommandManager.execute(player, command)
            }
        }
    }

    private fun broadcastChatSession(player: GeckoPlayer, session: RemoteChatSession) {
        PacketSendingUtils.broadcastPlayPacket(player.chatSessionInfoPacket(session.asData()))
    }

    private fun handlePlayerTick(event: PlayerTickEvent) {
        event.player.requireGeckoPlayer().chatHandler.tick()
    }

    private fun handlePlayerDisconnect(event: PlayerDisconnectEvent) {
        event.player.requireGeckoPlayer().chatHandler.close()
    }
}
