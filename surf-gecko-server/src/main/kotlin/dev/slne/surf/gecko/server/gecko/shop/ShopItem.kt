package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderEmergencyExitShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderScoutShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderShieldShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderSmokeBombShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.hider.HiderSpringShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.misc.MiscLightningShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionInvisShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.potions.PotionSpeedShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerHeartbeatKnifeShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerLaserShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerWebGrenadeShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerSonarShopItem
import dev.slne.surf.gecko.server.gecko.shop.items.seeker.SeekerSpyDeviceShopItem
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
                SeekerLaserShopItem,
                SeekerSonarShopItem,
                SeekerWebGrenadeShopItem,
                HiderSmokeBombShopItem,
                HiderScoutShopItem,
                HiderSpringShopItem,
                HiderShieldShopItem,
                HiderEmergencyExitShopItem
            )
        }

        fun byRole(role: GeckoGameRole) = items.filter { it.roles.contains(role) }
        fun byId(id: String) = items.find { it.id == id }
    }
}
