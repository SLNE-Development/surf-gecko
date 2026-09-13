package dev.slne.surf.gecko.server.gecko.lobby.riseup

import dev.slne.surf.api.core.messages.adventure.hasPermission
import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.permission.PermissionList
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent

object RiseUpListener : MinestomListener {
    @EventHandler
    fun onPunch(event: EntityAttackEvent) {
        val source = event.entity as? Player ?: return
        val target = event.target as? Player ?: return

        if(!GeckoLobby.contains(source) || !GeckoLobby.contains(target)) {
            return
        }

        if (target.uuid == source.uuid) {
            return
        }

        if (!source.isSneaking) {
            return
        }

        if (!source.hasPermission(PermissionList.RISEUP)) {
            return
        }

        target.scheduleNextTick {
            it.velocity = Vec(0.0, 18.0, 0.0)
        }

        listOf(target, source).forEach {
            it.playSound(GeckoSounds.LOBBY_RISEUP, Sound.Emitter.self())
        }
    }
}