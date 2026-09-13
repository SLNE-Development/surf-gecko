package dev.slne.surf.gecko.server.gecko.orbs

import dev.slne.surf.gecko.server.gecko.GeckoGame
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.ItemEntity
import java.time.Duration

open class GeckoOrb {
    fun spawn(game: GeckoGame, pos: Pos) {
        val entity = ItemEntity(GeckoOrbs.ITEM)

        entity.setPickupDelay(Duration.ofSeconds(0))
        entity.setInstance(game.instance, pos)
    }
}
