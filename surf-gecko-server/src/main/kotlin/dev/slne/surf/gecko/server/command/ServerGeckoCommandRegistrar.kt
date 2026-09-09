package dev.slne.surf.gecko.server.command

import dev.slne.surf.gecko.server.anticheat.command.antiCheatCommand
import dev.slne.surf.gecko.server.command.commands.gameModeCommand
import dev.slne.surf.gecko.server.command.commands.miniMessageCommand
import dev.slne.surf.gecko.server.command.commands.stopCommand
import dev.slne.surf.gecko.server.command.commands.teleportCommand
import dev.slne.surf.gecko.server.command.commands.statusBarCommand

object ServerGeckoCommandRegistrar {
    fun registerAll() {
        stopCommand()
        gameModeCommand()
        miniMessageCommand()
        teleportCommand()
        statusBarCommand()
        antiCheatCommand()
    }
}