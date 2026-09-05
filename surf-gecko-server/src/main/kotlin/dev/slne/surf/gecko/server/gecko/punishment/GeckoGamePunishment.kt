package dev.slne.surf.gecko.server.gecko.punishment

import java.time.OffsetDateTime
import java.util.*

data class GeckoGamePunishment(
    val playerUuid: UUID,
    val expiresAt: OffsetDateTime,
    val unpunished: Boolean,
    val punisherUuid: UUID?,
    val reason: String
)
