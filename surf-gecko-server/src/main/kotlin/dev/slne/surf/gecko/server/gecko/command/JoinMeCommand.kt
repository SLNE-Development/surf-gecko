package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.bitmap.common.head.composeHead
import dev.slne.surf.bitmap.common.head.renderHead
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.gecko.common.joinme.PublishJoinMeRedisEvent
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.gecko.server.permission.PermissionList
import dev.slne.surf.gecko.server.redis.redisApi
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component

fun joinMeCommand() = commandTree("joinme") {
    withPermission(PermissionList.COMMAND_JOINME)

    playerExecutor { player, _ ->
        val game = GeckoGameManager.findGame(player.uuid)

        if (game == null) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Du bist in keinem Spiel.")
            }
            return@playerExecutor
        }

        geckoAsyncScope.launch {
            val head = composeHead(
                renderHead(player.skin?.textures ?: "", 1), listOf(
                    Component.empty(),
                    Component.empty(),
                    buildText {
                        appendSpace()
                        appendSpace()
                        append(player.displayName ?: Component.text(player.username))
                        spacer(" spielt")
                    },
                    buildText {
                        appendSpace()
                        appendSpace()
                        geckoPrimary("Hide 'n Seek")
                        geckoSecondary(" auf ")
                        geckoHighlight(game.settings.map.mapDisplayName)
                    },
                    buildText {
                        appendSpace()
                        appendSpace()
                        white("👥 ")
                        spacer("${game.players.size}/${game.settings.maxPlayers}")
                        appendSpace()
                        white("\uD83D\uDCCD")
                        appendSpace()
                        spacer(SurfServer.current().displayName)
                    },
                    buildText {
                        appendSpace()
                        appendSpace()
                        geckoSecondary("» Klicke hier, um beizutreten!".toSmallCaps())
                    },
                    Component.empty(),
                    Component.empty(),
                )
            )

            redisApi.publishEvent(
                PublishJoinMeRedisEvent(
                    SurfServer.current(),
                    player.displayName ?: Component.text(player.username),
                    game.internalId,
                    buildText {
                        appendNewline()
                        append(head)
                        appendNewline()
                    }
                )
            )

            player.sendText {
                appendPrefix()
                geckoPrimary("Das Joinme wurde gesendet.")
            }
        }
    }
}