package dev.slne.surf.gecko.server.gecko.hotbar

import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

enum class GeckoHotbarAction {
    LOBBY,
    NEXT_ROUND;

    companion object {
        val TAG: Tag<String> = Tag.String("gecko_hotbar_action")

        fun of(item: ItemStack): GeckoHotbarAction? {
            val name = item.getTag(TAG) ?: return null

            return entries.firstOrNull { it.name == name }
        }
    }
}
