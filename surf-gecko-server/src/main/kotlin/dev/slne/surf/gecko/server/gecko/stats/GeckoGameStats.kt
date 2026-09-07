package dev.slne.surf.gecko.server.gecko.stats

import java.util.*

data class GeckoGameStats(
    val playerUuid: UUID,
    val gameId: ULong,
    val seeker: Boolean,
    val finalSeeker: Boolean,
    val win: Boolean,
    val kills: Int,
    val foundAfter: Long?
)
