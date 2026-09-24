package dev.slne.surf.gecko.server.database.table.events

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.dao.id.ULongIdTable

object GeckoEventDataTable : ULongIdTable("gecko_event_data") {
    val eventId = ulong("event_id").references(GeckoEventsTable.id)
    val key = varchar("key", 255)
    val value = largeText("value")
}