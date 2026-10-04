package dev.slne.surf.gecko.server.command.commands

import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.command.dsl.playersArgument
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.performance.monitor.StatusBarManager
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.entity.Player

fun statusBarCommand() = commandTree("statusbar") {
    withAliases("tpsbar", "rambar", "cpubar")
    withPermission(PermissionList.COMMAND_STATUSBAR)

    playerExecutor { player, _ ->
        notifyToggle(player, StatusBarManager.toggle(player))
    }

    playersArgument("targets") {
        withPermission(PermissionList.COMMAND_STATUSBAR_OTHERS)

        anyExecutor { sender, arguments ->
            val targets: Collection<Player> by arguments

            targets.forEach { target ->
                notifyToggle(target, StatusBarManager.toggle(target))
            }

            sender.sendTranslated(
                "command.statusbar.others",
                "players" to targets.joinToString(", ") { it.username }
            )
        }
    }
}

private fun notifyToggle(player: Player, visible: Boolean) =
    player.sendTranslated(if (visible) "command.statusbar.enabled" else "command.statusbar.disabled")
