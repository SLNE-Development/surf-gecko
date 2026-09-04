package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import java.util.*

object GeckoPlayerRoleSelector {
    fun selectRoles(uuids: Set<UUID>, settings: GeckoGameSettings): Map<UUID, GeckoGameRole> {
        val remainingUuids = mutableSetOf<UUID>().apply { addAll(uuids) }
        val roles = mutableMapOf<UUID, GeckoGameRole>()

        settings.forcedSeekers.forEach {
            if (remainingUuids.remove(it)) {
                roles[it] = GeckoGameRole.SEEKER
            }
        }

        settings.forcedHiders.forEach {
            if (remainingUuids.remove(it)) {
                roles[it] = GeckoGameRole.HIDER
            }
        }

        val hasSeeker = roles.values.any { it == GeckoGameRole.SEEKER }

        if (!hasSeeker && remainingUuids.isNotEmpty()) {
            val randomSeeker = remainingUuids.random()
            roles[randomSeeker] = GeckoGameRole.SEEKER
            remainingUuids.remove(randomSeeker)
        }

        remainingUuids.forEach {
            roles[it] = GeckoGameRole.HIDER
        }

        return roles
    }
}