package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.items.hider.*
import dev.slne.surf.gecko.server.gecko.shop.items.misc.MiscLightningShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionInvisShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionSpeedShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.*
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

interface ShopItem {
    val id: String
    val price: Int
    val displayName: String
    val description: String

    val item: ItemStack
    val inventoryItem: ItemStack get() = item.builder().withTag(ID_TAG, id).build()
    val displayItem: ItemStack get() = item
    val maps: List<GeckoMap>?
    val roles: List<GeckoGameRole>
    val types: List<ShopItemType>

    fun onUse(player: Player): Boolean

    fun availableOn(map: GeckoMap) = maps?.contains(map) != false

    companion object {
        val ID_TAG: Tag<String> = Tag.String("shop_item_id")

        private val items by lazy {
            listOf(
                PotionInvisShopItem,
                PotionSpeedShopItem,
                MiscLightningShopItem,
                SeekerHeartbeatKnifeShopItem,
                SeekerSpyDeviceShopItem,
                SeekerSonarShopItem,
                SeekerLaserShopItem,
                SeekerWebGrenadeShopItem,
                SeekerCompassShopItem,
                SeekerPowerBowShopItem,
                SeekerHiderCostumeShopItem,
                HiderSmokeBombShopItem,
                HiderScoutShopItem,
                HiderSpringShopItem,
                HiderShieldShopItem,
                HiderSeekerCostumeShopItem
            )
        }

        fun byType(type: ShopItemType) = items.filter { it.types.contains(type) }
        fun byRole(role: GeckoGameRole) = items.filter { it.roles.contains(role) }
        fun byId(id: String) = items.find { it.id == id }
    }
}
