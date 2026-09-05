package dev.slne.surf.gecko.server.gecko.shop.items

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import net.minestom.server.item.Material

object SeekerTestShopItem : ShopItem {
    override val id = "seeker-test"
    override val role = GeckoGameRole.SEEKER
    override val price = 5
    override val displayName = "Seeker Test Item"
    override val description = "This is a test item for seekers."
    override val displayMaterial: Material = Material.RED_DYE
}