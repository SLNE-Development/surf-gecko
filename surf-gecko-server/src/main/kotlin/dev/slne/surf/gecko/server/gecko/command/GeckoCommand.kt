package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutorSuspend
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.MinecraftServer

fun geckoCommand() = commandTree("gecko") {
    withPermission(PermissionList.COMMAND_GECKO)
    literalArgument("info") {
        anyExecutor { sender, _ ->
            sender.sendText {
                appendPrefix()
                geckoPrimary("GeckoGames: ${GeckoGameManager.getGames().size}")
            }
        }
    }

    literalArgument("join") {
        playerExecutorSuspend { player, _ ->
            val game = GeckoGameManager.selectGame(player)

            player.sendText {
                appendPrefix()
                if (game == null) {
                    geckoPrimary("Du konntest keinem Spiel zugewiesen werden.")
                } else {
                    geckoPrimary("Du wurdest einem Spiel zugewiesen.")
                }
            }
        }
    }

    literalArgument("queueAll") {
        playerExecutorSuspend { player, _ ->
            val gamePlayers = GeckoGameManager.playingPlayers()
            val players =
                MinecraftServer.getConnectionManager().onlinePlayers.filter { it.uuid !in gamePlayers }
                    .toList()

            players.forEach {
                GeckoGameManager.selectGame(it)
            }

            player.sendText {
                appendPrefix()
                geckoPrimary("Alle Spieler wurden der Warteschlange hinzugefügt.")
            }
        }
    }

    literalArgument("lobby") {
        playerExecutorSuspend { player, _ ->
            GeckoLobby.join(player)

            player.sendText {
                appendPrefix()
                geckoPrimary("Du bist nun in der Lobby.")
            }
        }
    }
}