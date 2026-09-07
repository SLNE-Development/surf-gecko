package dev.slne.surf.gecko.server.gecko.stats

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.state.GeckoGameEndReason
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GeckoGameStatsTracker {
    private val entries = ConcurrentHashMap<UUID, Entry>()

    @Volatile
    private var roundStartMillis: Long? = null

    fun beginRound(players: Collection<GeckoGamePlayer>) {
        roundStartMillis = System.currentTimeMillis()

        players.forEach {
            entries[it.playerUuid] = Entry(seeker = it.role == GeckoGameRole.SEEKER)
        }
    }

    fun addKill(playerUuid: UUID) {
        val entry = entries[playerUuid] ?: return

        synchronized(entry) {
            if (entry.frozen) {
                return
            }

            entry.kills++
        }
    }

    fun kills(playerUuid: UUID) = entries[playerUuid]?.kills ?: 0

    fun markFound(playerUuid: UUID) {
        val entry = entries[playerUuid] ?: return
        val startedAt = roundStartMillis ?: return

        synchronized(entry) {
            if (entry.frozen) {
                return
            }

            entry.foundAfter = System.currentTimeMillis() - startedAt
            entry.frozen = true
        }
    }

    fun markSeekerTeam(playerUuid: UUID) {
        val entry = entries[playerUuid] ?: return
        synchronized(entry) { entry.finalSeeker = true }
    }

    fun markLeft(playerUuid: UUID) {
        val entry = entries[playerUuid] ?: return
        synchronized(entry) { entry.left = true }
    }

    fun collect(gameId: ULong, endReason: GeckoGameEndReason) = entries.map { (playerUuid, entry) ->
        synchronized(entry) {
            GeckoGameStats(
                playerUuid = playerUuid,
                gameId = gameId,
                seeker = entry.seeker,
                finalSeeker = entry.finalSeeker,
                win = hasWon(entry, endReason),
                kills = entry.kills,
                foundAfter = entry.foundAfter
            )
        }
    }

    private fun hasWon(entry: Entry, endReason: GeckoGameEndReason) = when {
        entry.left -> false
        entry.foundAfter != null -> false
        endReason == GeckoGameEndReason.SEEKER_WIN -> entry.finalSeeker
        endReason == GeckoGameEndReason.HIDER_WIN -> !entry.finalSeeker
        else -> false
    }

    private class Entry(val seeker: Boolean) {
        var finalSeeker = seeker
        var kills = 0
        var foundAfter: Long? = null
        var frozen = false
        var left = false
    }
}
