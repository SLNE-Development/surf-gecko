package dev.slne.surf.gecko.server.player

import net.minestom.server.entity.Player
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Player.requireGeckoPlayer(): GeckoPlayer {
    contract {
        returns() implies (this@requireGeckoPlayer is GeckoPlayer)
    }

    require(this is GeckoPlayer) { "Player $username is not a GeckoPlayer" }
    return this
}
