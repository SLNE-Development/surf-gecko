package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.gecko.server.database.table.GeckoGameStatsTable
import dev.slne.surf.gecko.server.gecko.stats.GeckoGameStats

object GeckoGameStatsRepository {
    suspend fun insertStats(stats: Collection<GeckoGameStats>) {
        if (stats.isEmpty()) {
            return
        }

        suspendTransaction {
            GeckoGameStatsTable.batchInsert(stats, shouldReturnGeneratedValues = false) { stat ->
                this[GeckoGameStatsTable.playerUuid] = stat.playerUuid
                this[GeckoGameStatsTable.gameId] = stat.gameId
                this[GeckoGameStatsTable.seeker] = stat.seeker
                this[GeckoGameStatsTable.finalSeeker] = stat.finalSeeker
                this[GeckoGameStatsTable.win] = stat.win
                this[GeckoGameStatsTable.kills] = stat.kills
                this[GeckoGameStatsTable.foundAfter] = stat.foundAfter
            }
        }
    }
}
