package dev.slne.surf.gecko.server.gecko.orbs

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import jakarta.inject.Singleton
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.PickupItemEvent

@Singleton
class GeckoOrbListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PickupItemEvent::class.java) { handlePickup(it) }
    }

    private fun handlePickup(event: PickupItemEvent) {
        val stack = event.itemStack

        if (!stack.hasTag(GeckoOrbs.ORB_TAG_KEY)) {
            return
        }

        val player = event.livingEntity as? Player ?: return
        val game = GeckoGameManager.findGame(player.uuid)
        val gamePlayer = game?.findGamePlayer(player.uuid)

        if (game == null || gamePlayer == null || !game.state.isGame() ||
            gamePlayer.role == GeckoGameRole.SPECTATOR || gamePlayer.awaitingRespawn
        ) {
            event.isCancelled = true
            return
        }

        if (!player.inventory.addItemStack(stack)) {
            event.isCancelled = true
            return
        }

        player.playSound(GeckoSounds.ORB_PICKUP, Sound.Emitter.self())
    }
}
