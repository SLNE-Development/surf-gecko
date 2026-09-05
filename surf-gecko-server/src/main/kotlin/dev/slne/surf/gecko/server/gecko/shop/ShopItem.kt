package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.items.HiderInvisShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.HiderSpeedShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.SeekerSpeedShopItem
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

interface ShopItem {
    val id: String
    val role: GeckoGameRole
    val price: Int
    val displayName: String
    val description: String
    val displayItem: ItemStack

    val inventoryItem: ItemStack
    fun onUse(player: Player)

    companion object {
        private val items = listOf(SeekerSpeedShopItem, HiderInvisShopItem, HiderSpeedShopItem)
        fun byRole(role: GeckoGameRole) = items.filter { it.role == role }
    }
}