package dev.slne.surf.gecko.server.gecko.shop.effect.compass

import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoEffect
import dev.slne.surf.gecko.server.gecko.util.GECKO_HIGHLIGHT
import dev.slne.surf.gecko.server.util.withTag
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.LodestoneTracker
import net.minestom.server.network.packet.server.play.data.WorldPos
import net.minestom.server.tag.Tag
import kotlin.time.Duration.Companion.milliseconds

class CompassEffect(
    private val player: Player,
    private val displayName: String,
    private val target: () -> Player?
) : GeckoEffect(500.milliseconds) {
    private var tracked: Point? = null

    override fun isActive() = player.isOnline

    override fun start() {
        val current = target()

        tracked = current?.position?.asBlockVec()
        player.inventory.addItemStack(compassFor(current))
    }

    override fun pulse() {
        val current = target()
        val position = current?.position?.asBlockVec()

        if (position == tracked) {
            return
        }

        tracked = position
        slot()?.let { player.inventory.setItemStack(it, compassFor(current)) }
    }

    override fun stop() {
        slot()?.let { player.inventory.setItemStack(it, ItemStack.AIR) }
    }

    private fun slot() = (0 until PlayerInventory.INNER_INVENTORY_SIZE)
        .firstOrNull { player.inventory.getItemStack(it).hasTag(COMPASS_TAG) }

    private fun compassFor(target: Player?): ItemStack {
        val builder = ItemStack.of(Material.COMPASS).builder()
            .withTag(COMPASS_TAG, true)
            .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
            .set(DataComponents.ITEM_NAME, text(displayName, GECKO_HIGHLIGHT))

        val instance = target?.instance

        if (instance != null) {
            builder.set(
                DataComponents.LODESTONE_TRACKER,
                LodestoneTracker(
                    WorldPos(instance.dimensionName, target.position.asBlockVec()),
                    false
                )
            )
        }

        return builder.build()
    }

    companion object {
        private val COMPASS_TAG: Tag<Boolean> = Tag.Boolean("gecko_shop_compass")
    }
}
