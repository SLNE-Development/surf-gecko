package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.type.shops.hiderShopView
import dev.slne.surf.gecko.server.gecko.shop.type.shops.seekerShopView
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
        if (!debounce(player)) {
            return
        }

        if (item.hasTag(ShopItem.ID_TAG)) {
            val shopItem = ShopItem.byId(item.getTag(ShopItem.ID_TAG)) ?: return

            if (shopItem.onUse(player)) {
                ShopPurchase.consume(player, shopItem)
            }

            return
        }

        if (!item.hasTag(itemTag)) {
            return
        }

        val game = GeckoGameManager.findGame(player.uuid) ?: return
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return

        if (!game.state.isGame()) {
            return
        }

        when(gamePlayer.role) {
            GeckoGameRole.SEEKER -> seekerShopView.open(player, mapOf("map" to game.settings.map))
            GeckoGameRole.HIDER -> hiderShopView.open(player, mapOf("map" to game.settings.map))
            GeckoGameRole.SPECTATOR -> Unit
        }
    }

    private val lastUseAt = ConcurrentHashMap<UUID, Long>()

    private fun debounce(player: Player): Boolean {
        val now = System.currentTimeMillis()
        var allowed = false

        lastUseAt.compute(player.uuid) { _, previous ->
            allowed = previous == null || now - previous >= USE_DEBOUNCE_MILLIS

            if (allowed) now else previous
        }

        return allowed
    }

    companion object {
        private const val USE_DEBOUNCE_MILLIS = 150L

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
