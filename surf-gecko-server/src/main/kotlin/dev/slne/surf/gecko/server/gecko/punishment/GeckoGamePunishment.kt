package dev.slne.surf.gecko.server.gecko.punishment

import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

data class GeckoGamePunishment(
    val playerUuid: UUID,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime?,
    val unpunished: Boolean,
    val punisherUuid: UUID?,
    val reason: String
) {
    val permanent get() = expiresAt == null

    val totalDuration: Duration?
        get() = expiresAt?.let { JavaDuration.between(createdAt, it).toKotlinDuration() }

    fun remaining(now: OffsetDateTime = OffsetDateTime.now()): Duration? =
        expiresAt?.let { JavaDuration.between(now, it).toKotlinDuration() }

    fun isActive(now: OffsetDateTime = OffsetDateTime.now()) =
        !unpunished && (expiresAt == null || expiresAt.isAfter(now))
}
