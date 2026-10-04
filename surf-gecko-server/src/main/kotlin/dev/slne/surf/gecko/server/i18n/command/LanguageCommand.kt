package dev.slne.surf.gecko.server.i18n.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
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
