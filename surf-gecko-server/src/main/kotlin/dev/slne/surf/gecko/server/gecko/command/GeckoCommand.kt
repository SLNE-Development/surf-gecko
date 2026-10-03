package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
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
import dev.slne.surf.gecko.server.i18n.sendTranslated
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
            sender.sendTranslated(
                "command.gecko.info",
                "games" to GeckoGameManager.getGames().map { it.gameInfo.toString() }
            )
        }
    }

    literalArgument("join") {
        playerExecutorSuspend { player, _ ->
            val game = GeckoGameManager.selectGame(player)

            player.sendTranslated(if (game == null) "command.gecko.join.failed" else "command.gecko.join.success")
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

            player.sendTranslated("command.gecko.queue-all")
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

            player.sendTranslated("command.lobby.joined")
        }
    }

    literalArgument("groups") {
        playerExecutor { player, _ ->
            player.sendTranslated(
                "command.gecko.groups",
                "groups" to SocialGroupManager.groups().map { "${it.key}=${it.value}" }
            )
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
                        player.sendTranslated("command.gecko.shop.unknown-role")
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

                player.sendTranslated("command.gecko.giveorbs", "amount" to amount)
            }
        }
    }

    literalArgument("endcurrent") {
        playerExecutor { player, _ ->
            val game = GeckoGameManager.findGame(player.uuid)
            if (game == null) {
                player.sendTranslated("command.not-in-game")
                return@playerExecutor
            }

            geckoAsyncScope.launch {
                GeckoGameManager.endGame(game, GeckoGameEndReason.MANUELL)

                player.sendTranslated("command.gecko.endcurrent")
            }
        }
    }

    literalArgument("getspeed") {
        playerExecutor { player, _ ->
            player.sendTranslated(
                "command.gecko.getspeed",
                "speed" to player.getAttribute(Attribute.MOVEMENT_SPEED).baseValue,
                "fov" to player.fieldViewModifier
            )
        }
    }

    literalArgument("unpunishself") {
        playerExecutorSuspend { player, _ ->
            if (GeckoPunishmentRepository.unpunishPlayer(player.uuid)) {
                player.sendTranslated("command.gecko.unpunishself.success")
            } else {
                player.sendTranslated("command.gecko.unpunishself.not-punished")
            }
        }
    }
}

private fun dumpItem(player: Player, all: Boolean) {
    val item = player.itemInMainHand

    if (item.isAir) {
        player.sendTranslated("command.gecko.dumpitem.empty-hand")
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

    player.sendTranslated("command.gecko.dumpitem.result", "item" to argument)
}

@Suppress("UNCHECKED_CAST")
private fun encodeComponent(component: DataComponent<*>, value: Any): BinaryTag =
    (component as DataComponent<Any>).encode(Transcoder.NBT, value).orElseThrow()