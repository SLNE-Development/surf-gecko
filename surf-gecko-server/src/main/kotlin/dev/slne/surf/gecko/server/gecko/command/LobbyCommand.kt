package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutorSuspend
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.visual.ScreenFade
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.permission.PermissionList

fun lobbyCommand() = commandTree("lobby") {
    withPermission(PermissionList.COMMAND_LOBBY)

    playerExecutorSuspend { player, _ ->
        ScreenFade.transition(listOf(player))
        GeckoLobby.join(player)

        player.sendTranslated("command.lobby.joined")
    }
}