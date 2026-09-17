package dev.slne.surf.gecko.server.performance

import net.kyori.adventure.key.Key
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

object EntityTickFilter {

    @Volatile
    private var disabledTypeIds = BooleanArray(0)

    @JvmStatic
    fun shouldTick(entity: Entity): Boolean {
        val disabled = disabledTypeIds
        val id = entity.entityType.id()
        return id < 0 || id >= disabled.size || !disabled[id]
    }
    
    fun configure(vararg keys: Key) {
        val types = keys.mapNotNull { EntityType.fromKey(it) }
        val highestId = types.maxOfOrNull(EntityType::id) ?: -1
        val disabled = BooleanArray(highestId + 1)

        types.forEach { type ->
            disabled[type.id()] = true
        }

        disabledTypeIds = disabled
    }
}