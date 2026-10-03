package dev.slne.surf.gecko.server.i18n.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.i18n.view.languageView
import dev.slne.surf.gecko.server.permission.PermissionList

fun languageCommand() = commandTree("lang") {
    withAliases("language", "sprache")
    withPermission(PermissionList.COMMAND_LANGUAGE)

    playerExecutor { player, _ ->
        languageView.open(player)
    }
}
