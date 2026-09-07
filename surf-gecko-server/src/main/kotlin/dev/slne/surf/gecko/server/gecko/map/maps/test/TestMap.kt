package dev.slne.surf.gecko.server.gecko.map.maps.test

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import net.minestom.server.coordinate.Pos
import java.time.OffsetDateTime
import java.util.*

object TestMap : GeckoMap {
    override val mapUuid: UUID = UUID.fromString("a2badd2e-514e-4f8f-926b-1ec730da0541")
    override val mapName = "test-map"
    override val mapDisplayName = "Test Map"
    override val mapAuthors = listOf(
        GeckoMap.MapAuthor(
            "TheBjoRedCraft",
            UUID.fromString("1c779cb1-3860-4e23-9cac-7f160b2acc61")
        )
    )
    override val mapLocations = GeckoMap.MapLocations(
        Pos(1313.5, 113.0, -1431.5, 90f, 0f),
        Pos(1279.5, 87.0, -1376.5, 56f, 19f),
        Pos(1209.5, 87.0, -1392.5, -73f, 0f),
        listOf()
    )
    override val submittedAt: OffsetDateTime = OffsetDateTime.now()
}