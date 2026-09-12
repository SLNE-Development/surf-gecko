package dev.slne.surf.gecko.server.chat

import dev.slne.minestom.lobby.api.chat.ChatRenderer
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.hasPermission
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.gecko.server.chat.packet.DeleteChatPacketModern
import dev.slne.surf.gecko.server.chat.packet.framed
import dev.slne.surf.gecko.server.chat.signature.PlayerChatMessage
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsAccess
import dev.slne.surf.gecko.server.permission.PermissionList
import dev.slne.surf.gecko.server.player.GeckoPlayer
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.command.ConsoleSender
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.message.ChatType

class ChatProcessor(
    private val player: LobbyPlayer,
    private val message: PlayerChatMessage,
) {
    private val originalMessage = message.decoratedContent()
    private val outgoing = OutgoingChatMessage.create(message)

    private var messageChanged = false
    private var formatChanged = false

    suspend fun process() {
        val players = ConnectionManager.onlinePlayers
        val viewers = ObjectLinkedOpenHashSet<Audience>(players.size + 1).apply {
            addAll(players.filter {
                SocialGroupManager.canChat(
                    player.uuid,
                    it.uuid
                )
            })
            add(Audiences.console())
        }

        val renderer = ChatRenderer { source, _, message, viewer ->
            buildText {
                if (viewer.hasPermission(PermissionList.DELETE_MESSAGE)) {
                    append {
                        darkSpacer("[")
                        error("✘")
                        darkSpacer("]")
                        appendSpace()
                        clickCallback {
                            deleteMessage(this@ChatProcessor.message.adventureView())
                        }
                        hoverEvent(buildText {
                            error("Nachricht löschen")
                        })
                    }
                }

                append(miniMessage.deserialize("${LuckPermsAccess.prefix(source.uuid)}${source.username}"))
                appendSpace()
                append(message)
            }
        }

        formatChanged = true
        complete(originalMessage, renderer, viewers)
    }

    private suspend fun complete(
        message: Component,
        renderer: ChatRenderer,
        viewers: Set<Audience>
    ) {
        val displayName = player.displayName()

        val useVanillaChatType = renderer is ChatRenderer.Default
        val chatType = BoundChatType(
            chatType = if (useVanillaChatType) ChatType.CHAT else GeckoChatTypes.raw,
            name = displayName
        )

        when {
            formatChanged -> if (renderer is ChatRenderer.ViewerUnaware) {
                val rendered = renderer.render(player, displayName, message)
                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            } else {
                broadcast(viewers, chatType, sendConcurrent = true) { viewer ->
                    renderer.render(player, displayName, message, viewer)
                }
            }

            messageChanged -> {
                val rendered = if (useVanillaChatType) {
                    message
                } else {
                    (renderer as ChatRenderer.ViewerUnaware).render(player, displayName, message)
                }

                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            }

            else -> broadcast(viewers, chatType, sendConcurrent = false, unsignedFor = null)
        }
    }

    private suspend fun broadcast(
        viewers: Set<Audience>,
        chatType: BoundChatType,
        sendConcurrent: Boolean,
        unsignedFor: (suspend (Audience) -> Component)?
    ) {
        if (viewers.isEmpty()) return
        if (viewers.size == 1) {
            sendTo(viewers.first(), chatType, unsignedFor?.invoke(viewers.first()))
        } else if (sendConcurrent) {
            supervisorScope {
                for (viewer in viewers) {
                    launch {
                        sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
                    }
                }
            }
        } else {
            for (viewer in viewers) {
                sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
            }
        }
    }

    private fun sendTo(viewer: Audience, chatType: BoundChatType, unsigned: Component?) {
        when (viewer) {
            is GeckoPlayer -> outgoing.sendToPlayer(
                viewer,
                filtered = false,
                chatType,
                unsigned
            )

            is ConsoleSender -> viewer.sendMessage(
                GlobalTranslator.render(
                    chatType.decorate(unsigned ?: outgoing.content),
                    MinestomAdventure.getDefaultLocale()
                )
            )

            is ForwardingAudience.Single -> sendTo(viewer.audience(), chatType, unsigned)

            else -> {
                val message =
                    if (unsigned == null) message else message.withUnsignedContent(unsigned)
                viewer.sendMessage(message.adventureView(), chatType.adventure())
            }
        }
    }


    private fun deleteMessage(signedMessage: SignedMessage) =
        MinecraftServer.getConnectionManager().onlinePlayers.forEach {
            it.sendPacket(
                DeleteChatPacketModern(
                    MessageSignature.Packed(
                        MessageSignature(
                            signedMessage.signature()?.bytes()
                        )
                    )
                ).framed()
            )
        }
}
