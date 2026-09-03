package dev.slne.surf.gecko.server.database.table

import dev.slne.surf.database.table.AuditableLongIdTable
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason

object GeckoGamesTable : AuditableLongIdTable("gecko_games") {
    val map = varchar("map", 255)
    val endReason = enumerationByName<GeckoGameEndReason>("end_reason", 50)
}