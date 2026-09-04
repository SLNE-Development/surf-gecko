package dev.slne.surf.gecko.server.gecko.orbs

import dev.slne.surf.gecko.server.gecko.GeckoGame
import net.minestom.server.component.DataComponentMap
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.ItemEntity
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialKeys
import net.minestom.server.tag.Tag
import java.time.Duration

@Suppress("UnstableApiUsage")
open class GeckoOrb {
    fun spawn(game: GeckoGame, pos: Pos) {
        val entity = ItemEntity(
            ItemStack.of(
                Material.fromKey(MaterialKeys.PAPER.key()),
                DataComponentMap.builder().set(DataComponents.ITEM_MODEL, "nexo:gecko/orb").build()
            )
        )

        entity.setTag(ORB_TAG_KEY, true)
        entity.setPickupDelay(Duration.ofSeconds(0))
        entity.setInstance(game.instance, pos)
    }

    companion object {
        val ORB_TAG_KEY: Tag<Boolean> = Tag.Boolean("gecko_orb")
    }
}
