package dev.slne.surf.gecko.server.command.commands

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.greedyStringArgument
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.gecko.server.permission.PermissionList

fun miniMessageCommand() = commandTree("parsetext") {
    withPermission(PermissionList.COMMAND_MINIMESSAGE)

    greedyStringArgument("message") {
        playerExecutor { player, arguments ->
            val message: String by arguments
            player.sendMessage(miniMessage.deserialize(message))
        }
    }
}