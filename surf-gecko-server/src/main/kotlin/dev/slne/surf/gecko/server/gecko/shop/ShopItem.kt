package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.items.misc.MiscLightningShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionInvisShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionSpeedShopItem
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

interface ShopItem {
    val id: String
    val price: Int
    val displayName: String
    val description: String
    val displayItem: ItemStack

    val maps: List<GeckoMap>?
    val roles: List<GeckoGameRole>

    val inventoryItem: ItemStack
    fun onUse(player: Player)

    companion object {
        val ID_TAG: Tag<String> = Tag.String("shop_item_id")

        private val items by lazy {
            listOf(PotionInvisShopItem, PotionSpeedShopItem, MiscLightningShopItem)
        }

        fun byRole(role: GeckoGameRole) = items.filter { it.roles.contains(role) }
        fun byId(id: String) = items.find { it.id == id }
    }
}