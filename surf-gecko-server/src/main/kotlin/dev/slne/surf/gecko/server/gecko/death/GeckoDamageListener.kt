package dev.slne.surf.gecko.server.gecko.death

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.visual.ScreenShake
import dev.slne.surf.gecko.server.i18n.sendTranslatedActionBar
import dev.slne.surf.gecko.server.i18n.translate
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
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

        if (game.lobbyPlayers.any { it.playerUuid == player.uuid }) {
            event.isCancelled = true
            return
        }

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

        if(game.countStats) {
            attackingGamePlayer?.let { game.statsTracker.addKill(it.playerUuid) }
        }

        val victimName = Component.text(gamePlayer.player.username, gamePlayer.role.color)
        val outcome = if (attackingGamePlayer?.role == GeckoGameRole.SEEKER) "found" else "killed"

        attackingGamePlayer?.player?.sendTranslatedActionBar("game.kill.actionbar.$outcome", "victim" to victimName)

        playKillSounds(game, gamePlayer, attackingGamePlayer)

        if (attackingGamePlayer != null) {
            game.sendTranslated(
                "game.kill.broadcast.$outcome",
                "victim" to victimName,
                "attacker" to Component.text(attackingGamePlayer.player.username, attackingGamePlayer.role.color)
            )
        } else {
            game.sendTranslated("game.kill.broadcast.died", "victim" to victimName)
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
            ScreenShake.play(it, 0.6, 12)
            val player = it
            it.showTitle {
                title = player.translate("game.kill.title").colorIfAbsent(victim.role.color)
                subtitle = if (killer != null) {
                    player.translate("game.kill.subtitle.killer", "killer" to killer.player.username)
                } else {
                    player.translate("game.kill.subtitle.died")
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
