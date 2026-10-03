package dev.slne.surf.gecko.server.command

import dev.slne.surf.gecko.server.command.commands.gameModeCommand
import dev.slne.surf.gecko.server.command.commands.miniMessageCommand
import dev.slne.surf.gecko.server.command.commands.stopCommand
import dev.slne.surf.gecko.server.command.commands.teleportCommand
import dev.slne.surf.gecko.server.command.commands.statusBarCommand
import dev.slne.surf.gecko.server.i18n.command.i18nCommand
import dev.slne.surf.gecko.server.i18n.command.languageCommand

object ServerGeckoCommandRegistrar {
    fun registerAll() {
        stopCommand()
        gameModeCommand()
        miniMessageCommand()
        teleportCommand()
        statusBarCommand()
        i18nCommand()
        languageCommand()
    }
}
