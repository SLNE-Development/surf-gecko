package dev.slne.surf.gecko.server.anticheat

import dev.slne.surf.gecko.server.anticheat.check.Violation
import dev.slne.surf.gecko.server.config.Config
import net.minestom.server.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AntiCheatTracker {

    private val players = ConcurrentHashMap<UUID, AntiCheatPlayer>()

    fun register(
        player: Player,
        settings: Config.AntiCheatConfig,
        report: (Player, Violation) -> Unit
    ): AntiCheatPlayer = players.computeIfAbsent(player.uuid) {
        AntiCheatPlayer(player, settings, report)
    }

    fun find(player: Player): AntiCheatPlayer? = players[player.uuid]

    fun remove(player: Player) {
        players.remove(player.uuid)
    }

    fun clear() {
        players.clear()
    }
}
