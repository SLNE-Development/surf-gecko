package dev.slne.surf.gecko.server.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.gecko.server.database.table.GeckoPlayerLanguagesTable
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.*

object GeckoPlayerLanguageRepository {
    suspend fun fetchLanguage(playerUuid: UUID): GeckoLanguage? = suspendTransaction {
        GeckoPlayerLanguagesTable
            .select(GeckoPlayerLanguagesTable.language)
            .where { GeckoPlayerLanguagesTable.playerUuid eq playerUuid }
            .map { GeckoLanguage.fromId(it[GeckoPlayerLanguagesTable.language]) }
            .firstOrNull()
    }

    suspend fun saveLanguage(playerUuid: UUID, language: GeckoLanguage): Unit = suspendTransaction {
        GeckoPlayerLanguagesTable.upsert {
            it[GeckoPlayerLanguagesTable.playerUuid] = playerUuid
            it[GeckoPlayerLanguagesTable.language] = language.id
        }
    }
}
