package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.SortOrder
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.count
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.sum
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.gecko.server.database.table.GeckoGameStatsTable
import dev.slne.surf.gecko.server.gecko.lobby.leaderbord.data.LeaderboardSummary
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

object GeckoLeaderboardRepository {
    suspend fun fetchWinTotals(): List<LeaderboardSummary> = suspendTransaction {
        val wins = GeckoGameStatsTable.id.count()

        GeckoGameStatsTable
            .select(GeckoGameStatsTable.playerUuid, wins)
            .where(GeckoGameStatsTable.win eq true)
            .groupBy(GeckoGameStatsTable.playerUuid)
            .orderBy(wins, SortOrder.DESC)
            .map { LeaderboardSummary(it[GeckoGameStatsTable.playerUuid], it[wins]) }
            .toList()
    }

    suspend fun fetchKillTotals(): List<LeaderboardSummary> = suspendTransaction {
        val kills = GeckoGameStatsTable.kills.sum()

        GeckoGameStatsTable
            .select(GeckoGameStatsTable.playerUuid, kills)
            .groupBy(GeckoGameStatsTable.playerUuid)
            .orderBy(kills, SortOrder.DESC)
            .map {
                LeaderboardSummary(
                    it[GeckoGameStatsTable.playerUuid],
                    (it.getOrNull(kills) ?: 0).toLong()
                )
            }
            .toList()
    }
}
