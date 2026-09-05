package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutorSuspend
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.MinecraftServer

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

    literalArgument("join") {
        playerExecutorSuspend { player, _ ->
            GeckoGameManager.selectGame(player)

            player.sendText {
                appendSuccessPrefix()
                success("Du wurdest einem Spiel zugewiesen.")
            }
        }
    }

    literalArgument("queueAll") {
        playerExecutorSuspend { player, _ ->
            val gamePlayers = GeckoGameManager.playingPlayers()
            val players = MinecraftServer.getConnectionManager().onlinePlayers.filter { it.uuid !in gamePlayers }.toList()

            players.forEach {
                GeckoGameManager.selectGame(it)
            }

            player.sendText {
                appendSuccessPrefix()
                success("Alle Spieler wurden der Warteschlange hinzugefügt.")
            }
        }
    }
}