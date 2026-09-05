package dev.slne.surf.gecko.map.creator.draft

import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.Location
import java.util.*

class GeckoMapDraft(
    val ownerUuid: UUID,
    var mapName: String,
    var mapDisplayName: String,
    var worldName: String,
) {
    val mapUuid: UUID = UUID.randomUUID()
    val authors = mutableObjectListOf<DraftAuthor>()

    private val singleLocations = EnumMap<GeckoPoiType, DraftPos>(GeckoPoiType::class.java)
    private val multiLocations = EnumMap<GeckoPoiType, ObjectList<DraftPos>>(GeckoPoiType::class.java)

    fun locations(type: GeckoPoiType): List<DraftPos> = if (type.multiple) {
        multiLocations[type] ?: emptyList()
    } else {
        singleLocations[type]?.let(::listOf) ?: emptyList()
    }

    fun single(type: GeckoPoiType): DraftPos? = singleLocations[type]

    fun place(type: GeckoPoiType, pos: DraftPos): Int {
        if (!type.multiple) {
            singleLocations[type] = pos
            return 1
        }

        val list = multiLocations.getOrPut(type) { mutableObjectListOf() }
        list.add(pos)
        return list.size
    }

    fun remove(type: GeckoPoiType, reference: Location, maxDistance: Double): DraftPos? {
        if (!type.multiple) {
            return singleLocations.remove(type)
        }

        val list = multiLocations[type] ?: return null
        val maxDistanceSquared = maxDistance * maxDistance
        var bestIndex = -1
        var bestDistance = Double.MAX_VALUE

        for (index in list.indices) {
            val distance = list[index].distanceSquaredTo(reference)
            if (distance <= maxDistanceSquared && distance < bestDistance) {
                bestIndex = index
                bestDistance = distance
            }
        }

        return if (bestIndex >= 0) list.removeAt(bestIndex) else null
    }

    fun clear(type: GeckoPoiType): Int {
        if (!type.multiple) {
            return if (singleLocations.remove(type) != null) 1 else 0
        }

        val removed = multiLocations[type]?.size ?: 0
        multiLocations.remove(type)
        return removed
    }

    fun locationsByType(): Map<GeckoPoiType, List<DraftPos>> =
        GeckoPoiType.entries.associateWith { locations(it) }

    fun missingTypes() = GeckoPoiType.entries.filter { locations(it).isEmpty() }

    fun totalPois() = GeckoPoiType.entries.sumOf { locations(it).size }

    data class DraftAuthor(val name: String, val uuid: UUID)
}

