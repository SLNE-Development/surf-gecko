package dev.slne.surf.gecko.server.command

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.command.commandapi.dsl.anyExecutor
import dev.slne.surf.gecko.command.commandapi.dsl.commandTree
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.MinecraftServer

fun stopCommand() = commandTree("stop") {
    withAliases("exit", "shutdown")
    withPermission(PermissionList.COMMAND_STOP)

    anyExecutor { sender, _ ->
        sender.sendText {
            appendInfoPrefix()
            info("Der Server wird heruntergefahren...")
        }

        MinecraftServer.stopCleanly()
    }
}