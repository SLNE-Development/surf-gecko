package dev.slne.surf.gecko.server.gecko

import dev.slne.surf.gecko.server.database.GeckoDatabaseManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val geckoLogger: Logger = LoggerFactory.getLogger("GeckoGames")

object GeckoInstance {
    suspend fun enable() {
        geckoLogger.info("Enabling GeckoInstance...")
        GeckoGameManager.init()
        GeckoDatabaseManager.create()

        geckoLogger.info("Enabled GeckoInstance.")
    }

    suspend fun shutdown() {
        geckoLogger.info("Stopping GeckoInstance...")
        GeckoGameManager.shutdown()
        GeckoDatabaseManager.shutdown()

        geckoLogger.info("Stopped GeckoInstance.")
    }
}