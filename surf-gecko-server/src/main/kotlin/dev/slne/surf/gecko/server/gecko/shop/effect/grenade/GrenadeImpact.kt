package dev.slne.surf.gecko.server.gecko.shop.effect.grenade

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance

data class GrenadeImpact(
    val instance: Instance,
    val position: Pos,
    val thrower: Player
)