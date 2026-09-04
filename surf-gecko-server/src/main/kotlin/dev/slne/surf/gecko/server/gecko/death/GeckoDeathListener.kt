package dev.slne.surf.gecko.server.gecko.death

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDamageEvent

@Singleton
class GeckoDeathListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(EntityDamageEvent::class.java) { handleDamage(it) }
    }

    private fun handleDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val game = GeckoGameManager.findGame(player.uuid) ?: return
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return

        if (gamePlayer.awaitingRespawn) {
            event.isCancelled = true
            return
        }

        val attacker = event.damage.attacker as? Player
        val attackingGamePlayer = attacker?.let { game.findGamePlayer(it.uuid) }

        if (attackingGamePlayer != null && game.isTeamDamage(attackingGamePlayer, gamePlayer)) {
            event.isCancelled = true
            return
        }

        if (event.damage.amount < player.health) {
            return
        }

        event.isCancelled = true
        game.handleDeath(gamePlayer)
    }
}
