package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.*

class LeaderboardHologram(playerUuid: UUID) {
    private val entity = Entity(EntityType.TEXT_DISPLAY)

    init {
        entity.updateViewableRule { it.uuid == playerUuid }
        entity.setNoGravity(true)
        entity.editEntityMeta<TextDisplayMeta> { meta ->
            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.FIXED
            meta.isShadow = true
            meta.backgroundColor = 0x60000000
        }
    }

    fun spawn(instance: Instance, position: Pos) {
        entity.setInstance(instance, position)
    }

    fun update(text: Component) {
        entity.editEntityMeta<TextDisplayMeta> { meta -> meta.text = text }
    }

    fun remove() {
        entity.remove()
    }
}
