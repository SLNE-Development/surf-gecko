package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.gecko.server.combat.bowConsumeOnShotTag
import dev.slne.surf.gecko.server.combat.bowFixedDamageTag
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.util.GECKO_HIGHLIGHT
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.item.enchant.Enchantment

object SeekerPowerBowShopItem : ShopItem {
    override val id = "seeker_power_bow"
    override val price = 12
    override val displayName = "Power Bogen"
    override val description = "Ein Schuss, der 5 Herzen Schaden macht"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_ATTACK)

    override val item: ItemStack = ItemStack.of(Material.BOW).builder()
        .set(DataComponents.ENCHANTMENTS, EnchantmentList(mapOf(Enchantment.INFINITY to 1)))
        .set(
            DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay(false, setOf(DataComponents.ENCHANTMENTS))
        )
        .set(DataComponents.ITEM_NAME, text(displayName, GECKO_HIGHLIGHT))
        .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
        .withTag(bowFixedDamageTag, 10f)
        .withTag(bowConsumeOnShotTag, true)
        .build()

    override fun onUse(player: Player) = false
}
