package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.gecko.server.database.table.GeckoGamesTable
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason

object GeckoGameRepository {
    suspend fun saveGame(settings: GeckoGameSettings) = suspendTransaction {
        GeckoGamesTable.insertAndGetId {
            it[map] = settings.map.mapUuid
        }.value
    }

    suspend fun updateGameEndReason(game: GeckoGame, endReason: GeckoGameEndReason) =
        suspendTransaction {
            GeckoGamesTable.update(where = { GeckoGamesTable.id eq game.internalId }) {
                it[GeckoGamesTable.endReason] = endReason
            }
        }
}