package dev.slne.surf.gecko.server.database.table.events

import dev.slne.surf.database.table.AuditableLongIdTable
import dev.slne.surf.gecko.server.gecko.events.GeckoEvent

object GeckoEventsTable : AuditableLongIdTable("gecko_events") {
    val eventType = enumerationByName<GeckoEvent.Type>("event_type", 50)
}