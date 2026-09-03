package dev.slne.surf.gecko.command.platform

import dev.slne.surf.gecko.command.commandapi.CommandAPI
import dev.slne.surf.gecko.command.internal.extension.CommandManager

/**
 * Installs and uninstalls the Minestom platform the command API compiles onto.
 *
 * [start] has to run before any command is registered and [stop] once the server shuts down;
 * a registration made while no platform is installed is rejected. Wire both into whatever
 * lifecycle the host owns - the port carries no lifecycle interface of its own, only the two
 * suspending entry points.
 */
class MinestomCommandAPIService(
    private val ownership: MinestomCommandOwnership,
) {
    private var platform: MinestomCommandAPIPlatform? = null

    suspend fun start() {
        check(platform == null) { "Minestom CommandAPI is already installed" }

        CommandAPITranslations.register()

        val installed = MinestomCommandAPIPlatform(CommandManager, ownership)
        CommandAPI.installPlatform(installed)
        platform = installed
    }

    suspend fun stop() {
        val installed = platform ?: return
        platform = null
        try {
            installed.close()
        } finally {
            CommandAPI.uninstallPlatform(installed)
        }
    }
}
