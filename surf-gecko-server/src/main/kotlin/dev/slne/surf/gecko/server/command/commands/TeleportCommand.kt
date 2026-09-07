package dev.slne.surf.gecko.server.command.commands

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.entitiesArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.entityArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.minestom.lobby.api.command.entity.displayName
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player

fun teleportCommand() = commandTree("teleport") {
    withPermission(PermissionList.COMMAND_TELEPORT)
    withAliases("tp")

    entityArgument("target") {
        playerExecutor { player, arguments ->
            val target: Entity by arguments

            player.teleport(target.position).thenRun {
                player.sendText {
                    appendSuccessPrefix()
                    success("Du wurdest zu ${target.display} teleportiert.")
                }
            }
        }
    }

    entitiesArgument("targets") {
        entityArgument("destination") {
            playerExecutor { player, arguments ->
                val targets: List<Entity> by arguments
                val destination: Entity by arguments

                targets.forEach { target ->
                    target.teleport(destination.position).thenRun {
                        player.sendText {
                            appendSuccessPrefix()
                            success("${target.display} wurde zu ${destination.display} teleportiert.")
                        }
                    }
                }
            }
        }
    }
}

private val Entity.display
    get() = when (this) {
        is Player -> this.username
        is CommandSender -> "Non Player Command Sender"
        is Entity -> this.displayName
    }