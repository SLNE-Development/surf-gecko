package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.gecko.common.joinme.PublishJoinMeRedisEvent
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.permission.PermissionList
import dev.slne.surf.gecko.server.redis.redisApi
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

        redisApi.publishEvent(
            PublishJoinMeRedisEvent(
                SurfServer.current(),
                player.displayName ?: Component.text(player.username),
                game.internalId,
                buildText {
                    appendPrefix()
                    append(player.displayName ?: Component.text(player.username))
                    append(geckoHighlight(" lädt dich ein, dem Spiel beizutreten!"))

                    appendNewline()
                    appendPrefix()
                    geckoPrimary("Hide 'n Seek auf ")
                    geckoHighlight(game.settings.map.mapDisplayName)
                }
            ))

        player.sendText {
            appendPrefix()
            geckoPrimary("Das Joinme wurde gesendet.")
        }
    }
}