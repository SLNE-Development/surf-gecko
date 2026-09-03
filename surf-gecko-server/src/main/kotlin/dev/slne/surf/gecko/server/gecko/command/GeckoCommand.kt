package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.permission.PermissionList

fun geckoCommand() = commandTree("gecko") {
    withPermission(PermissionList.COMMAND_GECKO)
    literalArgument("info") {
        anyExecutor { sender, _ ->
            sender.sendText {
                appendInfoPrefix()
                info("GeckoGames: ${GeckoGameManager.getGames().size}")
            }
        }
    }
}