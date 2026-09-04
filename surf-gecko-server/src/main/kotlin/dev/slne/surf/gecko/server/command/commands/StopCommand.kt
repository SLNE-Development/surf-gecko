package dev.slne.surf.gecko.server.command.commands

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.GeckoBootstrap
import dev.slne.surf.gecko.server.GeckoServer
import dev.slne.surf.gecko.server.permission.PermissionList

fun stopCommand() = commandTree("stop") {
    withAliases("exit", "shutdown")
    withPermission(PermissionList.COMMAND_STOP)

    anyExecutor { sender, _ ->
        sender.sendText {
            appendInfoPrefix()
            info("Der Server wird heruntergefahren...")
        }

        GeckoBootstrap.injector.getInstance(GeckoServer::class.java).beginShutdown()
    }
}