package dev.slne.surf.gecko.server.gecko.shop.items

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.potion.PotionType

object HiderSpeedShopItem : ShopItem {
    override val id = "hider_speed"
    override val role = GeckoGameRole.HIDER
    override val price = 5
    override val displayName = "Geschwindigkeitstrank"
    override val description = "Werde für 5 Sekunden schneller"
    override val displayItem: ItemStack = ItemStack.builder(Material.POTION)
        .set(
            DataComponents.POTION_CONTENTS, PotionContents(
                PotionType.SWIFTNESS
            )
        )
        .build()
    override val inventoryItem: ItemStack = ItemStack.builder(Material.POTION)
        .set(
            DataComponents.POTION_CONTENTS, PotionContents(
                PotionType.SWIFTNESS
            )
        )
        .build()

    override fun onUse(player: Player) {
        player.addEffect(Potion(PotionEffect.INVISIBILITY, 1, 10 * 20))
        player.playSound(true) {
            type(key("minecraft:entity.generic.drink"))
        }
    }
}