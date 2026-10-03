package dev.slne.surf.gecko.server.gecko.map

import dev.slne.surf.gecko.server.gecko.map.mechanic.GeckoMapMechanic
import dev.slne.surf.gecko.server.i18n.translatable
import net.minestom.server.coordinate.Pos
import java.time.OffsetDateTime
import java.util.*

interface GeckoMap {
    val mapUuid: UUID
    val mapName: String
    val mapDisplayName: String
    val mapAuthors: List<MapAuthor>
    val mapContributors: List<MapAuthor>
    val mapLocations: MapLocations
    val mechanics: List<GeckoMapMechanic>

    val submittedAt: OffsetDateTime

    val displayName get() = translatable("map.$mapName.name")

    data class MapAuthor(
        val name: String,
        val uuid: UUID,
    )

    data class MapLocations(
        val lobbySpawn: Pos,
        val seekerSpawn: Pos,
        val spawn: Pos,
        val orbSpawns: List<Pos>
    )
}
