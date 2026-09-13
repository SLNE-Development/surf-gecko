package dev.slne.surf.gecko.server.gecko.shop.effect

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import kotlinx.coroutines.launch
import net.minestom.server.entity.Player
import kotlin.time.Duration

fun ShopItem.playOnce(player: Player, duration: Duration, effect: GeckoEffect): Boolean {
    if (!ShopItemUsages.claim(id, player)) {
        return false
    }

    geckoScope.launch {
        try {
            effect.playFor(duration)
        } finally {
            ShopItemUsages.release(id, player)
        }
    }

    return true
}