package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.inList
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.gecko.server.database.table.GeckoPlayerNamesTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.util.*

object GeckoPlayerNameRepository {
    suspend fun saveName(playerUuid: UUID, name: String): Unit = suspendTransaction {
        GeckoPlayerNamesTable.upsert {
            it[GeckoPlayerNamesTable.playerUuid] = playerUuid
            it[GeckoPlayerNamesTable.name] = name
        }
    }

    suspend fun fetchNames(playerUuids: Collection<UUID>): Map<UUID, String> {
        if (playerUuids.isEmpty()) {
            return emptyMap()
        }

        return suspendTransaction {
            GeckoPlayerNamesTable
                .select(GeckoPlayerNamesTable.playerUuid, GeckoPlayerNamesTable.name)
                .where { GeckoPlayerNamesTable.playerUuid inList playerUuids.toList() }
                .map { it[GeckoPlayerNamesTable.playerUuid] to it[GeckoPlayerNamesTable.name] }
                .toList()
                .toMap()
        }
    }
}
