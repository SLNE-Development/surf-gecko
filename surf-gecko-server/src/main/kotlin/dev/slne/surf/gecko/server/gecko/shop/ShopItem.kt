package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.items.HiderTestShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.SeekerTestShopItem
import net.minestom.server.item.Material

interface ShopItem {
    val id: String
    val role: GeckoGameRole
    val price: Int
    val displayName: String
    val description: String
    val displayMaterial: Material

    companion object {
        private val items = listOf(SeekerTestShopItem, HiderTestShopItem)
        fun byRole(role: GeckoGameRole) = items.filter { it.role == role }
    }
}