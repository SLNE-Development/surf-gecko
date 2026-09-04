package dev.slne.surf.gecko.server.command

import dev.slne.surf.gecko.server.command.commands.gameModeCommand
import dev.slne.surf.gecko.server.command.commands.stopCommand

object ServerGeckoCommandRegistrar {
    fun registerAll() {
        stopCommand()
        gameModeCommand()
    }
}