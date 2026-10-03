package dev.slne.surf.gecko.server.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.table.AuditableTable

object GeckoPlayerLanguagesTable : AuditableTable("gecko_player_languages") {
    val playerUuid = nativeUuid("player_uuid").uniqueIndex()
    val language = varchar("language", 16)
}
