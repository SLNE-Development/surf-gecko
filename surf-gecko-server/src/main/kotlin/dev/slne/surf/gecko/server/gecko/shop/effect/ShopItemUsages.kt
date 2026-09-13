package dev.slne.surf.gecko.server.gecko.shop.effect

import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ShopItemUsages {
    private val active = ConcurrentHashMap.newKeySet<Usage>()

    fun claim(itemId: String, player: Player) = active.add(Usage(itemId, player.uuid))
    fun release(itemId: String, player: Player) {
        active.remove(Usage(itemId, player.uuid))
    }

    data class Usage(val itemId: String, val player: UUID)
}
