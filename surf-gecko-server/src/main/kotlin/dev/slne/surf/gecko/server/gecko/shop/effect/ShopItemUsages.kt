package dev.slne.surf.gecko.server.gecko.shop.effect

import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object ShopItemUsages {
    private val active = ConcurrentHashMap.newKeySet<Pair<String, UUID>>()

    fun start(itemId: String, player: Player) = active.add(itemId to player.uuid)

    fun finish(itemId: String, player: Player) = active.remove(itemId to player.uuid)
}
