package dev.slne.surf.gecko.server.database

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.gecko.server.database.table.GeckoGameStatsTable
import dev.slne.surf.gecko.server.database.table.GeckoGamesTable
import dev.slne.surf.gecko.server.database.table.GeckoPunishmentsTable
import kotlin.io.path.Path

object GeckoDatabaseManager {
    private lateinit var databaseApi: DatabaseApi

    suspend fun create() {
        databaseApi = DatabaseApi.create(Path("."))

        suspendTransaction {
            SchemaUtils.create(GeckoGamesTable, GeckoGameStatsTable, GeckoPunishmentsTable)
        }
    }

    fun shutdown() {
        if(::databaseApi.isInitialized) {
            databaseApi.shutdown()
        }
    }
}