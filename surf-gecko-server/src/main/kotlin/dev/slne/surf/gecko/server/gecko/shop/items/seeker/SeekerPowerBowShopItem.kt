package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.combat.bowConsumeOnShotTag
import dev.slne.surf.gecko.server.combat.bowFixedDamageTag
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
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
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_ATTACK)

    override val item: ItemStack = ItemStack.of(Material.BOW).builder()
        .set(DataComponents.ENCHANTMENTS, EnchantmentList(mapOf(Enchantment.INFINITY to 1)))
        .set(
            DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay(false, setOf(DataComponents.ENCHANTMENTS))
        )
        .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
        .withTag(bowFixedDamageTag, 10f)
        .withTag(bowConsumeOnShotTag, true)
        .build()

    override fun localize(stack: ItemStack, language: GeckoLanguage): ItemStack = stack.with(
        DataComponents.ITEM_NAME,
        GeckoTranslations.render(language, "shop.item.inventory.name", "name" to displayName)
    )

    override fun onUse(player: Player) = false
}
