package dev.slne.surf.gecko.server.command.commands

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.gameModeArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playersArgument
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

fun gameModeCommand() = commandTree("gamemode") {
    withAliases("gm")
    withPermission(PermissionList.COMMAND_GAMEMODE)

    gameModeArgument("gamemode") {
        playerExecutor { player, arguments ->
            val gamemode: GameMode by arguments

            player.setGameMode(gamemode)
            player.sendTranslated(
                "command.gamemode.self",
                "gamemode" to gamemode.name.lowercase().replaceFirstChar { it.uppercase() }
            )
        }

        playersArgument("targets") {
            playerExecutor { player, arguments ->
                val gamemode: GameMode by arguments
                val targets: Collection<Player> by arguments

                targets.forEach { it.setGameMode(gamemode) }

                player.sendTranslated(
                    "command.gamemode.others",
                    "players" to targets.joinToString(", ") { it.username },
                    "gamemode" to gamemode.name.lowercase().replaceFirstChar { it.uppercase() }
                )
            }
        }
    }
}