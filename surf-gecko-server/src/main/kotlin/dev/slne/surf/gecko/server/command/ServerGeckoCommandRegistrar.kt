package dev.slne.surf.gecko.server.command

import dev.slne.surf.gecko.server.command.commands.gameModeCommand
import dev.slne.surf.gecko.server.command.commands.miniMessageCommand
import dev.slne.surf.gecko.server.command.commands.stopCommand
import dev.slne.surf.gecko.server.command.commands.teleportCommand

object ServerGeckoCommandRegistrar {
    fun registerAll() {
        stopCommand()
        gameModeCommand()
        miniMessageCommand()
        teleportCommand()
    }
}