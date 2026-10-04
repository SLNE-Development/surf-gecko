package dev.slne.surf.gecko.server.gecko.lobby.listener

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent

class GeckoLobbyListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockPlaceEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockBreakEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerBlockInteractEvent::class.java) { cancel(it, it.player) }
        node.addListener(ItemDropEvent::class.java) { cancel(it, it.player) }
        node.addListener(PlayerUseItemEvent::class.java) { cancel(it, it.player) }
        node.addListener(EntityDamageEvent::class.java) {
            cancel(
                it,
                it.entity as? Player ?: return@addListener
            )
        }
        node.addListener(AsyncPlayerPreLoginEvent::class.java) {
            if (!GeckoLobby.initialized) {
                it.connection.kick(GeckoTranslations.render(GeckoLanguage.FALLBACK, "login.lobby-not-initialized"))
            }
        }
    }

    private fun cancel(event: CancellableEvent, player: Player) {
        if (player.instance != GeckoLobby.instance) {
            return
        }

        if (hasBypass(player)) {
            return
        }

        event.isCancelled = true
    }

    private fun hasBypass(player: Player) = player.gameMode == GameMode.CREATIVE
}