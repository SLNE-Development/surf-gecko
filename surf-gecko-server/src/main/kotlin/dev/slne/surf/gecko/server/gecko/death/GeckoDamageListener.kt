package dev.slne.surf.gecko.server.gecko.death

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextDecoration
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
            spacer(" gefunden")
        })

        playKillSounds(game, gamePlayer, attackingGamePlayer)

        game.sendText {
            appendInfoPrefix()
            text(gamePlayer.player.username, gamePlayer.role.color)

            if (attackingGamePlayer != null) {
                spacer(" wurde von ")
                text(attackingGamePlayer.player.username, attackingGamePlayer.role.color)
                spacer(" gefunden")
            } else {
                spacer(" ist gestorben")
            }
        }

        event.isCancelled = true
        game.handleDeath(gamePlayer)
    }

    private fun playKillSounds(
        game: GeckoGame,
        victim: GeckoGamePlayer,
        killer: GeckoGamePlayer?
    ) {
        killer?.playerOrNull?.let {
            it.playSound(GeckoSounds.KILL_CRIT, Sound.Emitter.self())
            it.playSound(GeckoSounds.KILL_CONFIRM, Sound.Emitter.self())
        }

        victim.playerOrNull?.let {
            it.playSound(GeckoSounds.DEATH_SELF, Sound.Emitter.self())
            it.showTitle {
                title {
                    text("Gefunden", victim.role.color, TextDecoration.BOLD)
                }
                subtitle {
                    if (killer != null) {
                        info("Du wurdest von ")
                        variableValue(killer.player.username)
                        info(" erwischt")
                    } else {
                        info("Du bist gestorben")
                    }
                }
                times {
                    fadeIn(2)
                    stay(30)
                    fadeOut(10)
                }
            }
        }

        game.forEachPlayer {
            if (it.uuid == victim.playerUuid || it.uuid == killer?.playerUuid) {
                return@forEachPlayer
            }

            it.playSound(GeckoSounds.DEATH_BROADCAST, Sound.Emitter.self())
        }
    }
}
