package dev.slne.surf.gecko.server.gecko.death

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDamageEvent

@Singleton
class GeckoDamageListener : EventRegistrar {
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

        if (!game.state.isGame()) {
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

        attackingGamePlayer?.player?.sendActionBar(buildText {
            spacer("Du hast ")
            variableValue(gamePlayer.player.username)
            spacer(" getötet")
        })

        attackingGamePlayer?.player?.playSound(true) {
            type(key("minecraft:entity.player.attack.crit"))
        }

        game.sendText {
            appendInfoPrefix()
            text(gamePlayer.player.username, gamePlayer.role.color)

            if (attackingGamePlayer != null) {
                spacer(" wurde von ")
                text(attackingGamePlayer.player.username, attackingGamePlayer.role.color)
                spacer(" getötet")
            } else {
                spacer(" ist gestorben")
            }
        }

        event.isCancelled = true
        game.handleDeath(gamePlayer)
    }
}
