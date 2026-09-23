package dev.slne.surf.gecko.server.gecko.visual

import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.minestom.server.color.Color
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.CustomModelData

internal fun Instance.shaderTick(delayTicks: Int) = ((worldAge + delayTicks) % 24000).toInt()

internal val Int.high get() = (this shr 8) and 0xFF
internal val Int.low get() = this and 0xFF

internal fun spawnVisualProbe(
    instance: Instance,
    position: Pos,
    viewer: Player,
    model: String,
    tints: List<Color>,
    cullSize: Float,
    viewRange: Float
): Entity {
    val item = ItemStack.builder(Material.PAPER)
        .set(DataComponents.ITEM_MODEL, model)
        .set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(listOf(), listOf(), listOf(), tints))
        .build()

    return Entity(EntityType.ITEM_DISPLAY).apply {
        setNoGravity(true)
        isGlowing = true
        updateViewableRule { it.uuid == viewer.uuid }
        editEntityMeta<ItemDisplayMeta> { meta ->
            meta.itemStack = item
            meta.displayContext = ItemDisplayMeta.DisplayContext.NONE
            meta.width = cullSize
            meta.height = cullSize
            meta.viewRange = viewRange
            meta.scale = Vec(1.0E-4)
        }
        setInstance(instance, position)
    }
}
