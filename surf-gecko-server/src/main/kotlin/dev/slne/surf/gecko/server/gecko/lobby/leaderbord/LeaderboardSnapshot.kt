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
            val sorted = totals.sortedWith(
                compareByDescending<LeaderboardSummary> { it.value }
                    .thenBy { names[it.playerUuid] ?: "Unbekannt" }
            )

            val places = HashMap<UUID, LeaderboardPlacement>(sorted.size)

            sorted.forEachIndexed { index, total ->
                places[total.playerUuid] = LeaderboardPlacement(
                    rank = index + 1,
                    value = total.value
                )
            }

            val top = sorted.take(topSize).map { total ->
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
