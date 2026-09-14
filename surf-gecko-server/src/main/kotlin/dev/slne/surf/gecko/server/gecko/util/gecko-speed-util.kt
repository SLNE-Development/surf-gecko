package dev.slne.surf.gecko.server.gecko.util

import dev.slne.surf.gecko.server.gecko.map.mechanic.VentMechanic
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute

val Player.defaultMovementSpeed: Double
    get() = entityType.defaultAttributes()[Attribute.MOVEMENT_SPEED]
        ?: Attribute.MOVEMENT_SPEED.defaultValue()

fun Player.applyMovementSpeedFactor(factor: Double) {
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = defaultMovementSpeed * factor
    fieldViewModifier = (defaultMovementSpeed * factor).toFloat()
}

fun Player.resetGeckoSpeed() {
    VentMechanic.clearVentState(this)
    applyMovementSpeedFactor(1.0)
}
