package dev.slne.surf.gecko.server.gecko

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.surf.gecko.server.antiesp.PlayerCulling
import dev.slne.surf.gecko.server.database.GeckoDatabaseManager
import dev.slne.surf.gecko.server.gecko.command.geckoCommand
import dev.slne.surf.gecko.server.gecko.command.lobbyCommand
import dev.slne.surf.gecko.server.gecko.command.skipCommand
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.tablist.GeckoGameTablistManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val geckoLogger: Logger = LoggerFactory.getLogger("GeckoGames")

object GeckoInstance {
    suspend fun enable() {
        geckoLogger.info("Enabling GeckoInstance...")
        GeckoDatabaseManager.create()
        GeckoLobby.createLobby()
        GeckoGameManager.init()
        GeckoGameTablistManager.init()
        PlayerCulling.init()

        geckoCommand()
        skipCommand()

        CommandAPI.unregister("lobby")
        lobbyCommand()

        geckoLogger.info("Enabled GeckoInstance.")
    }

    suspend fun shutdown() {
        geckoLogger.info("Stopping GeckoInstance...")
        GeckoGameManager.shutdown()
        GeckoGameTablistManager.shutdown()
        PlayerCulling.shutdown()
        GeckoDatabaseManager.shutdown()

        geckoLogger.info("Stopped GeckoInstance.")
    }
}