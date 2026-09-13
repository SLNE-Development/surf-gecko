package dev.slne.surf.gecko.server.gecko.lobby.elytra

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

object ElytraBoostTracker {
    private val boostingPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val boostCooldowns = Caffeine.newBuilder()
        .expireAfterWrite(2.seconds)
        .build<UUID, Unit>()

    fun isBoosting(uuid: UUID): Boolean = boostingPlayers.contains(uuid)
    fun startBoosting(uuid: UUID): Boolean = boostingPlayers.add(uuid)
    fun isOnCooldown(uuid: UUID): Boolean = boostCooldowns.getIfPresent(uuid) != null

    fun markBoosted(uuid: UUID) {
        boostCooldowns.put(uuid, Unit)
    }

    fun clear(uuid: UUID): Boolean = boostingPlayers.remove(uuid)
}