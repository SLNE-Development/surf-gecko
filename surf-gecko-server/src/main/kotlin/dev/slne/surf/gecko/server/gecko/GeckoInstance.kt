package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.gecko.server.database.GeckoDatabaseManager
import dev.slne.surf.gecko.server.gecko.command.geckoCommand
import dev.slne.surf.gecko.server.gecko.map.convert.GeckoMapConverter
import dev.slne.surf.gecko.server.gecko.tablist.GeckoGameTablistManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val geckoLogger: Logger = LoggerFactory.getLogger("GeckoGames")

object GeckoInstance {
    suspend fun enable() {
        geckoLogger.info("Enabling GeckoInstance...")
        GeckoMapConverter.convertAll()
        GeckoDatabaseManager.create()
        GeckoGameManager.init()
        GeckoGameTablistManager.init()

        geckoCommand()

        geckoLogger.info("Enabled GeckoInstance.")
    }

    suspend fun shutdown() {
        geckoLogger.info("Stopping GeckoInstance...")
        GeckoGameManager.shutdown()
        GeckoGameTablistManager.shutdown()
        GeckoDatabaseManager.shutdown()

        geckoLogger.info("Stopped GeckoInstance.")
    }
}