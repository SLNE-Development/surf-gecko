package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.util.random
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.database.repository.GeckoPunishmentRepository
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.map.GeckoMaps
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbs
import dev.slne.surf.gecko.server.gecko.shop.type.shops.hiderShopView
import dev.slne.surf.gecko.server.gecko.shop.type.shops.seekerShopView
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.gecko.server.permission.PermissionList
import kotlinx.coroutines.launch
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.TagStringIO
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponent
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.item.ItemStack

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

    literalArgument("dumpitem") {
        playerExecutor { player, _ -> dumpItem(player, all = false) }

        literalArgument("all") {
            playerExecutor { player, _ -> dumpItem(player, all = true) }
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

                when (type) {
                    "seeker" -> seekerShopView.open(
                        player,
                        mapOf("map" to GeckoMaps.random(), "seed" to random.nextLong())
                    )

                    "hider" -> hiderShopView.open(
                        player,
                        mapOf("map" to GeckoMaps.random(), "seed" to random.nextLong())
                    )

                    else -> {
                        player.sendText {
                            appendPrefix()
                            geckoPrimary("Die Rolle wurde nicht gefunden.")
                        }
                        return@playerExecutor
                    }
                }
            }
        }
    }

    literalArgument("giveorbs") {
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

    literalArgument("getspeed") {
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

    literalArgument("unpunishself") {
        playerExecutorSuspend { player, _ ->
            if (GeckoPunishmentRepository.unpunishPlayer(player.uuid)) {
                player.sendText {
                    appendPrefix()
                    geckoPrimary("Du wurdest erfolgreich entbannt.")
                }
            } else {
                player.sendText {
                    appendPrefix()
                    geckoPrimary("Du bist nicht gebannt oder wurdest bereits entbannt.")
                }
            }
        }
    }
}

private fun dumpItem(player: Player, all: Boolean) {
    val item = player.itemInMainHand

    if (item.isAir) {
        player.sendText {
            appendPrefix()
            geckoPrimary("Du hältst kein Item in der Hand.")
        }
        return
    }

    val components: CompoundBinaryTag = if (all) {
        val builder = CompoundBinaryTag.builder()
        for (entry in item.components().entrySet()) {
            val value = entry.value() ?: continue
            builder.put(
                entry.component().key().asString(),
                encodeComponent(entry.component(), value)
            )
        }
        builder.build()
    } else {
        (ItemStack.CODEC.encode(Transcoder.NBT, item).orElseThrow() as CompoundBinaryTag)
            .getCompound("components")
    }

    val id = item.material().key().asString()
    val argument = if (components.size() == 0) {
        id
    } else {
        val body = TagStringIO.tagStringIO().asString(components)
        "$id[${body.substring(1, body.length - 1)}]"
    }

    player.sendText {
        appendPrefix()
        geckoPrimary("Das Item: ")
        geckoSecondary(argument)
    }
}

@Suppress("UNCHECKED_CAST")
private fun encodeComponent(component: DataComponent<*>, value: Any): BinaryTag =
    (component as DataComponent<Any>).encode(Transcoder.NBT, value).orElseThrow()