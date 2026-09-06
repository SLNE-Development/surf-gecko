package dev.slne.surf.gecko.server.gecko.punishment

import dev.slne.surf.gecko.server.database.repository.GeckoPunishmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minestom.server.entity.Player
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object GeckoGamePunisher {
    private val durations = listOf(
        5.minutes,
        15.minutes,
        60.minutes,
        12.hours,
        1.days,
        1.5.days,
        2.days,
        3.days,
        5.days,
        7.days,
        10.days,
        14.days,
        21.days,
        30.days,
        60.days,
        90.days,
        180.days,
        null
    )

    suspend fun punish(player: Player, reason: String = "Fehlverhalten"): GeckoGamePunishment =
        punish(player.uuid, reason).also { GeckoPunishmentService.apply(player, it) }

    suspend fun punish(playerUuid: UUID, reason: String = "Fehlverhalten"): GeckoGamePunishment =
        withContext(Dispatchers.IO) {
            val level = GeckoPunishmentRepository.countPunishments(playerUuid)
            val now = OffsetDateTime.now()

            val punishment = GeckoGamePunishment(
                playerUuid = playerUuid,
                createdAt = now,
                expiresAt = nextDuration(level)?.let { now.plus(it.toJavaDuration()) },
                unpunished = false,
                punisherUuid = null,
                reason = reason
            )

            GeckoPunishmentRepository.insertPunishment(punishment)
            punishment
        }

    private fun nextDuration(previousPunishments: Long) =
        durations[previousPunishments.coerceIn(0, durations.lastIndex.toLong()).toInt()]
}
