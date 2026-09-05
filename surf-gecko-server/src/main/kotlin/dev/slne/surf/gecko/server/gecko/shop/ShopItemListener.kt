package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.util.withTag
import jakarta.inject.Singleton
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

@Singleton
class ShopItemListener : EventRegistrar {
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
        if (!item.hasTag(itemTag)) {
            return
        }

        val game = GeckoGameManager.findGame(player.uuid) ?: return
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return

        if (!game.state.isGame()) {
            return
        }

        shopView.open(player, mapOf("role" to gamePlayer.role))
    }

    companion object {
        val itemTag: Tag<Boolean> = Tag.Boolean("shop_item")

        fun giveShop(gamePlayer: GeckoGamePlayer) {
            gamePlayer.player.inventory.setItemStack(
                8, ItemStack.of(Material.CHEST).builder()
                    .withTag(itemTag, true)
                    .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
                    .set(DataComponents.ITEM_NAME, buildText {
                        append(gamePlayer.role.displayText)
                        text(" Shop", gamePlayer.role.color)
                    }).build()
            )
        }
    }
}
