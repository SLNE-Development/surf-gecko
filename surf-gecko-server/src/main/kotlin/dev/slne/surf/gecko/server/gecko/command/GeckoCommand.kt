package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.map.GeckoMaps
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbs
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.shopView
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.gecko.server.permission.PermissionList
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.attribute.Attribute

fun geckoCommand() = commandTree("gecko") {
    withPermission(PermissionList.COMMAND_GECKO)
    literalArgument("info") {
        anyExecutor { sender, _ ->
            sender.sendText {
                appendPrefix()
                geckoPrimary(
                    "GeckoGames: ${
                        GeckoGameManager.getGames().map { it.gameInfo.toString() }
                    }"
                )
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

    literalArgument("groups") {
        playerExecutor { player, _ ->
            player.sendText {
                appendPrefix()
                geckoPrimary(
                    "Gruppen: ${
                        SocialGroupManager.groups().map { "${it.key}=${it.value}" }
                    }"
                )
            }
        }
    }

    literalArgument("shop") {
        multiLiteralArgument("type", "seeker", "hider") {
            playerExecutor { player, arguments ->
                val type: String by arguments

                val role = when (type) {
                    "seeker" -> GeckoGameRole.SEEKER
                    "hider" -> GeckoGameRole.HIDER
                    else -> null
                }

                if (role == null) {
                    player.sendText {
                        appendPrefix()
                        geckoPrimary("Die Rolle wurde nicht gefunden.")
                    }
                    return@playerExecutor
                }

                val map = GeckoMaps.random()

                shopView.open(player, mapOf("map" to map, "role" to role))
            }
        }
    }

    literalArgument("giveOrbs") {
        integerArgument("amount") {
            playerExecutor { player, arguments ->
                val amount: Int by arguments

                GeckoOrbs.give(player, amount)

                player.sendText {
                    appendPrefix()
                    geckoPrimary("Du hast $amount Orbs erhalten.")
                }
            }
        }
    }

    literalArgument("endcurrent") {
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
                GeckoGameManager.endGame(game, GeckoGameEndReason.MANUELL)

                player.sendText {
                    appendPrefix()
                    geckoPrimary("Das aktuelle Spiel wurde beendet.")
                }
            }
        }
    }

    literalArgument("speed") {
        playerExecutor { player, _ ->
            player.sendText {
                appendPrefix()
                geckoPrimary("Deine aktuelle Geschwindigkeit liegt bei ")
                geckoSecondary(player.getAttribute(Attribute.MOVEMENT_SPEED).baseValue.toString())
                geckoPrimary(" und deine FOV Modifier liegt bei ")
                geckoSecondary(player.fieldViewModifier.toString())
            }
        }
    }
}