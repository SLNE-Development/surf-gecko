package dev.slne.surf.gecko.server.command.commands

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playersArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.performance.tps.TpsBarManager
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.entity.Player

fun tpsBarCommand() = commandTree("tpsbar") {
    withPermission(PermissionList.COMMAND_TPSBAR)

    playerExecutor { player, _ ->
        val visible = TpsBarManager.toggle(player)

        player.sendText {
            appendSuccessPrefix()
            success("Die TPS-Anzeige wurde ")
            variableValue(if (visible) "aktiviert" else "deaktiviert")
            success(".")
        }
    }

    playersArgument("targets") {
        withPermission(PermissionList.COMMAND_TPSBAR_OTHERS)

        anyExecutor { sender, arguments ->
            val targets: Collection<Player> by arguments

            targets.forEach { target ->
                val visible = TpsBarManager.toggle(target)

                target.sendText {
                    appendSuccessPrefix()
                    success("Die TPS-Anzeige wurde ")
                    variableValue(if (visible) "aktiviert" else "deaktiviert")
                    success(".")
                }
            }

            sender.sendText {
                appendSuccessPrefix()
                success("Die TPS-Anzeige wurde für ")
                variableValue(targets.joinToString(", ") { it.username })
                success(" umgeschaltet.")
            }
        }
    }
}
