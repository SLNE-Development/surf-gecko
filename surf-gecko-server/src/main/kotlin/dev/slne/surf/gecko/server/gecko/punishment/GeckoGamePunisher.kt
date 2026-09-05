package dev.slne.surf.gecko.server.gecko.punishment

import net.minestom.server.entity.Player
import java.time.OffsetDateTime

object GeckoGamePunisher {
    suspend fun punish(player: Player): GeckoGamePunishment {
        return GeckoGamePunishment(
            player.uuid,
            OffsetDateTime.now(),
            true,
            player.uuid,
            "Punished by GeckoGamePunisher"
        )
    }
}