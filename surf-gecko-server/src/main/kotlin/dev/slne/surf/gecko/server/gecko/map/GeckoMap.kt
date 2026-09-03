package dev.slne.surf.gecko.server.gecko.map

import net.minestom.server.coordinate.Pos
import java.time.OffsetDateTime
import java.util.*

data class GeckoMap(
    val mapUuid: UUID,
    val mapName: String,
    val mapDisplayName: String,
    val mapAuthors: List<MapAuthor>,
    val mapLocations: MapLocations,

    val submittedAt: OffsetDateTime,
) {
    data class MapAuthor(
        val name: String,
        val uuid: UUID,
    )

    data class MapLocations(
        val lobbySpawn: Pos,
        val seekerSpawn: Pos,
        val spawn: Pos,
    )
}
