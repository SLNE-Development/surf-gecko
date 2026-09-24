package dev.slne.surf.gecko.server.gecko.hotbar

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.visual.ScreenFade
import jakarta.inject.Singleton
import kotlinx.coroutines.launch
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
class GeckoHotbarListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(
            EventListener.builder(PlayerUseItemEvent::class.java)
                .ignoreCancelled(false)
                .handler { handleInteract(it.player, it.itemStack) }
                .build()
        )
        node.addListener(
            EventListener.builder(PlayerBlockInteractEvent::class.java)
                .ignoreCancelled(false)
                .handler { handleInteract(it.player, it.player.getItemInHand(it.hand)) }
                .build()
        )
        node.addListener(PlayerEntityInteractEvent::class.java) {
            handleInteract(it.player, it.player.getItemInHand(it.hand))
        }
    }

    private fun handleInteract(player: Player, item: ItemStack) {
        val action = GeckoHotbarAction.of(item) ?: return

        if (!debounce(player)) {
            return
        }

        geckoAsyncScope.launch {
            ScreenFade.transition(listOf(player))
            when (action) {
                GeckoHotbarAction.LOBBY -> GeckoLobby.join(player)
                GeckoHotbarAction.NEXT_ROUND -> joinNextRound(player)
            }
        }
    }

    private suspend fun joinNextRound(player: Player) {
        GeckoGameManager.handleGameLeave(player)
        GeckoGameManager.clearDirtyData(player.uuid)

        if (GeckoGameManager.selectGame(player) != null) {
            return
        }

        GeckoLobby.join(player)

        player.sendText {
            appendPrefix()
            geckoPrimary("Es ist aktuell keine Runde verfügbar.")
        }
    }

    private val lastUseAt = ConcurrentHashMap<UUID, Long>()

    private fun debounce(player: Player): Boolean {
        val now = System.currentTimeMillis()
        var allowed = false

        lastUseAt.compute(player.uuid) { _, previous ->
            allowed = previous == null || now - previous >= 500L

            if (allowed) now else previous
        }

        return allowed
    }
}
