package dev.slne.surf.gecko.server.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.table.AuditableLongIdTable

object GeckoGameStatsTable : AuditableLongIdTable("gecko_game_stats") {
    val playerUuid = nativeUuid("player_uuid")
    val gameId = ulong("game_id")

    val seeker = bool("seeker")
    val finalSeeker = bool("final_seeker")
    val win = bool("win")
    val kills = integer("kills")
    val foundAfter = long("found_after").nullable()
}