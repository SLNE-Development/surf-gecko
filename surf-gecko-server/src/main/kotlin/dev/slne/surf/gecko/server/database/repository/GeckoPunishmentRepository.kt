package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.*
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.gecko.server.database.table.GeckoPunishmentsTable
import dev.slne.surf.gecko.server.gecko.punishment.GeckoGamePunishment
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime
import java.util.*

object GeckoPunishmentRepository {
    suspend fun fetchActivePunishment(playerUuid: UUID): GeckoGamePunishment? = suspendTransaction {
        val now = OffsetDateTime.now()

        GeckoPunishmentsTable
            .selectAll()
            .where {
                (GeckoPunishmentsTable.playerUuid eq playerUuid) and
                        (GeckoPunishmentsTable.unpunished eq false) and
                        (
                                GeckoPunishmentsTable.expiresAt.isNull() or
                                        (GeckoPunishmentsTable.expiresAt greater now)
                                )
            }
            .orderBy(GeckoPunishmentsTable.createdAt, SortOrder.DESC)
            .limit(1)
            .map {
                GeckoGamePunishment(
                    playerUuid = it[GeckoPunishmentsTable.playerUuid],
                    createdAt = it[GeckoPunishmentsTable.createdAt],
                    expiresAt = it[GeckoPunishmentsTable.expiresAt],
                    unpunished = it[GeckoPunishmentsTable.unpunished],
                    punisherUuid = it[GeckoPunishmentsTable.punisherUuid],
                    reason = it[GeckoPunishmentsTable.reason]
                )
            }
            .firstOrNull()
    }

    suspend fun countPunishments(playerUuid: UUID): Long = suspendTransaction {
        GeckoPunishmentsTable
            .selectAll()
            .where {
                (GeckoPunishmentsTable.playerUuid eq playerUuid) and
                        (GeckoPunishmentsTable.unpunished eq false)
            }
            .count()
    }

    suspend fun insertPunishment(punishment: GeckoGamePunishment): Unit = suspendTransaction {
        GeckoPunishmentsTable.insert {
            it[playerUuid] = punishment.playerUuid
            it[createdAt] = punishment.createdAt
            it[expiresAt] = punishment.expiresAt
            it[unpunished] = punishment.unpunished
            it[punisherUuid] = punishment.punisherUuid
            it[reason] = punishment.reason
        }
    }
}
