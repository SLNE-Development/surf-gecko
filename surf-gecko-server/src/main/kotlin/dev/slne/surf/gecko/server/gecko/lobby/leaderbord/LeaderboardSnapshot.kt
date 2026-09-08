package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import dev.slne.surf.gecko.server.gecko.lobby.leaderbord.data.LeaderboardPlacement
import dev.slne.surf.gecko.server.gecko.lobby.leaderbord.data.LeaderboardSummary
import dev.slne.surf.gecko.server.gecko.lobby.leaderbord.data.LeaderboardTopEntry
import java.util.*


class LeaderboardSnapshot(
    val top: List<LeaderboardTopEntry>,
    private val places: Map<UUID, LeaderboardPlacement>
) {
    fun place(playerUuid: UUID) = places[playerUuid]

    companion object {
        val EMPTY = LeaderboardSnapshot(emptyList(), emptyMap())

        fun of(
            totals: List<LeaderboardSummary>,
            names: Map<UUID, String>,
            topSize: Int
        ): LeaderboardSnapshot {
            val places = HashMap<UUID, LeaderboardPlacement>(totals.size)
            var rank = 0
            var lastValue: Long? = null

            totals.forEachIndexed { index, total ->
                if (total.value != lastValue) {
                    rank = index + 1
                    lastValue = total.value
                }

                places[total.playerUuid] = LeaderboardPlacement(rank, total.value)
            }

            val top = totals.take(topSize).map { total ->
                LeaderboardTopEntry(
                    rank = places.getValue(total.playerUuid).rank,
                    name = names[total.playerUuid] ?: "Unbekannt",
                    value = total.value
                )
            }

            return LeaderboardSnapshot(top, places)
        }
    }
}
