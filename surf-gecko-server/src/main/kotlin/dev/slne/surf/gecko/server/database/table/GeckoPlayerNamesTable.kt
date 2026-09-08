package dev.slne.surf.gecko.server.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.table.AuditableTable

object GeckoPlayerNamesTable : AuditableTable("gecko_player_names") {
    val playerUuid = nativeUuid("player_uuid").uniqueIndex()
    val name = varchar("name", 16)
}
