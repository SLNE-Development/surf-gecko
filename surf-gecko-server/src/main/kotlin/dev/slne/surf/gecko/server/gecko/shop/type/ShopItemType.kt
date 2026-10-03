package dev.slne.surf.gecko.server.gecko.shop.type

import dev.slne.surf.gecko.server.gecko.util.removeItalics
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.TooltipDisplay

enum class ShopItemType(private val nameKey: String, private val base: ItemStack, private val customName: Boolean = false) {
    HIDER_ATTACK("shop.category.attack.name", ItemStack.of(Material.IRON_SWORD)),
    HIDER_DEFENSE("shop.category.defense.name", ItemStack.of(Material.SHIELD)),
    HIDER_TROLL("shop.category.troll.name", ItemStack.of(Material.CARROT_ON_A_STICK)),
    HIDER_POTION(
        "shop.category.potion.name",
        ItemStack.builder(Material.POTION)
            .set(
                DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(
                    false, setOf(
                        DataComponents.POTION_CONTENTS,
                        DataComponents.POTION_DURATION_SCALE
                    )
                )
            )
            .build(),
        true
    ),

    SEEKER_ATTACK("shop.category.attack.name", ItemStack.of(Material.IRON_SWORD)),
    SEEKER_SEARCH("shop.category.search.name", ItemStack.of(Material.BOW)),
    SEEKER_POTION(
        "shop.category.potion.name",
        ItemStack.builder(Material.POTION)
            .set(
                DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(
                    false, setOf(
                        DataComponents.POTION_CONTENTS,
                        DataComponents.POTION_DURATION_SCALE
                    )
                )
            )
            .build(),
        true
    );

    fun item(language: GeckoLanguage): ItemStack {
        val name = GeckoTranslations.render(language, nameKey)

        return if (customName) {
            base.with(DataComponents.CUSTOM_NAME, name.removeItalics())
        } else {
            base.with(DataComponents.ITEM_NAME, name)
        }
    }
}
