package dev.slne.surf.gecko.server.gecko.shop.items

import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import net.minestom.server.item.Material

object HiderTestShopItem : ShopItem {
    override val id = "hider-test"
    override val role = GeckoGameRole.HIDER
    override val price = 5
    override val displayName = "Hider Test Item"
    override val description = "This is a test item for hiders."
    override val displayMaterial: Material = Material.BLUE_DYE
}