package dev.slne.surf.gecko.server.antiesp

import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerXRay {
    private val targets = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    fun add(viewer: Player, target: Player) {
        targets.computeIfAbsent(viewer.uuid) { ConcurrentHashMap.newKeySet() }.add(target.uuid)
    }

    fun remove(viewer: Player, target: Player) {
        val known = targets[viewer.uuid] ?: return
        known.remove(target.uuid)

        if (known.isEmpty()) {
            targets.remove(viewer.uuid)
        }
    }

    fun targetsOf(viewer: Player): Set<UUID> = targets[viewer.uuid] ?: emptySet()

    fun clear() = targets.clear()
}
