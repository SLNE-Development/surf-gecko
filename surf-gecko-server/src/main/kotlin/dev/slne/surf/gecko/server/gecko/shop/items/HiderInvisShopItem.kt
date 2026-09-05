package dev.slne.surf.gecko.server.gecko.shop.items

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.potion.PotionType

object HiderInvisShopItem : ShopItem {
    override val id = "hider_invisibility"
    override val role = GeckoGameRole.HIDER
    override val price = 5
    override val displayName = "Unsichtbarkeitstrank"
    override val description = "Werde für 10 Sekunden unsichtbar"
    override val displayItem: ItemStack = ItemStack.builder(Material.POTION)
        .set(
            DataComponents.POTION_CONTENTS, PotionContents(
                PotionType.INVISIBILITY
            )
        )
        .build()
    override val inventoryItem: ItemStack = ItemStack.builder(Material.POTION)
        .set(
            DataComponents.POTION_CONTENTS, PotionContents(
                PotionType.INVISIBILITY
            )
        )
        .withTag(ShopItem.ID_TAG, id)
        .build()

    override fun onUse(player: Player) {
        player.addEffect(Potion(PotionEffect.INVISIBILITY, 1, 10 * 20))
        player.playSound(true) {
            type(key("minecraft:entity.generic.drink"))
        }
    }
}