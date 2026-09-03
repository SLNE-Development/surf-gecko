package dev.slne.surf.gecko.server.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.columns.time.offsetDateTime
import dev.slne.surf.database.table.AuditableLongIdTable

object GeckoPunishmentsTable : AuditableLongIdTable("gecko_punishments") {
    val playerUuid = nativeUuid("player_uuid")
    val expiresAt = offsetDateTime("expires_at").nullable()
    val unpunished = bool("unpunished").default(false)
    val punisherUuid = nativeUuid("punisher_uuid").nullable()
    val reason = varchar("reason", 255)
}