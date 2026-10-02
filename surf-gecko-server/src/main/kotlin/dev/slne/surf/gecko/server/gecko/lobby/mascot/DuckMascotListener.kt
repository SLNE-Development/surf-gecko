package dev.slne.surf.gecko.server.gecko.lobby.mascot

import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent

object DuckMascotListener : MinestomListener {
    @EventHandler
    fun onInteract(event: PlayerEntityInteractEvent) {
        if (event.hand != PlayerHand.MAIN || event.target !is DuckMascot) {
            return
        }

        DuckMascotManager.talk(event.player)
    }

    @EventHandler
    fun onAttack(event: EntityAttackEvent) {
        val player = event.entity as? Player ?: return

        if (event.target !is DuckMascot) {
            return
        }

        DuckMascotManager.talk(player)
    }
}
