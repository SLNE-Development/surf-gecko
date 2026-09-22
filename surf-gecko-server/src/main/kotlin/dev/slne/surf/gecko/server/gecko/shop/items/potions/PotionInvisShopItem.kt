package dev.slne.surf.gecko.server.gecko.shop.items.potions

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.PotionContents
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.potion.PotionType

object PotionInvisShopItem : ShopItem {
    override val id = "invisibility_potion"
    override val roles = listOf(GeckoGameRole.HIDER)
    override val price = 5
    override val displayName = "Unsichtbarkeitstrank"
    override val description = "Werde für 10 Sekunden unsichtbar"
    override val item: ItemStack = ItemStack.builder(Material.POTION)
        .set(
            DataComponents.POTION_CONTENTS, PotionContents(
                PotionType.INVISIBILITY
            )
        )
        .set(
            DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay(false, setOf(DataComponents.POTION_CONTENTS))
        )
        .build()
    override val maps = null

    override fun onUse(player: Player): Boolean {
        player.addEffect(Potion(PotionEffect.INVISIBILITY, 1, 10 * 20))
        player.playSound(true) {
            type(key("minecraft:entity.generic.drink"))
        }

        return true
    }
}