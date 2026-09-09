package dev.slne.surf.gecko.server.anticheat.simulation

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec

data class SimulationState(
    val position: Pos,
    val velocity: Vec,
    val onGround: Boolean,
    val collided: Boolean = false,
)
