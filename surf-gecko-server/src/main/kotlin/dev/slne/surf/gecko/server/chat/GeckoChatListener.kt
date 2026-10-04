package dev.slne.surf.gecko.server.chat

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.displayName
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatRenderer
import dev.slne.surf.api.minestom.chat.deleteSignedMessage
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsAccess
import dev.slne.surf.gecko.server.permission.PermissionList
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.entity.Player

object GeckoChatListener {
    fun register() {
        AsyncPlayerChatEvent.addListener { event ->
            val sender = event.player

            event.viewers.removeIf { viewer ->
                viewer is Player && !SocialGroupManager.canChat(sender.uuid, viewer.uuid)
            }

            event.renderer = ChatRenderer { source, _, message, viewer ->
                buildText {
                    if (viewer.canDeleteMessages()) {
                        append {
                            darkSpacer("[")
                            error("✘")
                            darkSpacer("]")
                            appendSpace()
                            clickCallback {
                                deleteMessage(event.signedMessage)

                                ConnectionManager.onlinePlayers
                                    .filter { it.hasPermission(PermissionList.DELETE_MESSAGE) }
                                    .forEach {
                                        it.sendMessage(
                                            it.translate(
                                                "chat.gecko.delete.notify",
                                                "deleter" to viewer.displayName(),
                                                "sender" to source.displayName()
                                            ).hoverEvent(buildText {
                                                append(message).colorIfAbsent(Colors.WHITE)
                                            })
                                        )
                                    }
                            }
                            hoverEvent(viewer.translate("chat.gecko.delete.hover"))
                        }
                    }

                    append(miniMessage.deserialize("${LuckPermsAccess.prefix(source.uuid)}${source.username}"))
                    darkSpacer(":")
                    appendSpace()
                    append(message)
                }
            }
        }
    }

    private fun Audience.canDeleteMessages() =
        this is Player && hasPermission(PermissionList.DELETE_MESSAGE)

    private fun deleteMessage(message: SignedMessage) {
        val signature = message.signature() ?: return
        ConnectionManager.onlinePlayers.forEach { it.deleteSignedMessage(signature) }
    }
}
