package dev.slne.surf.gecko.server.gecko.punishment

import net.minestom.server.entity.Player
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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

    suspend fun punish(player: Player, reason: String = "Fehlverhalten"): GeckoGamePunishment {
        return GeckoGamePunishment(
            player.uuid,
            OffsetDateTime.now(),
            true,
            player.uuid,
            reason
        )
    }
}