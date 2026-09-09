package dev.slne.surf.gecko.server.anticheat.simulation

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.WorldBorder
import net.minestom.server.instance.block.Block

class SimulationContext(
    val blockGetter: Block.Getter,
    val worldBorder: WorldBorder,
    val boundingBox: BoundingBox,
    val yaw: Float,
    val lookY: Double,
    val gravity: Double,
    val movementSpeed: Double,
    val flyingSpeed: Double,
    val blockFriction: Double,
    val blockSpeedFactor: Double,
    val jumpPower: Double,
    val flying: Boolean,
    val inWater: Boolean,
    val fluidHeight: Double,
    val fluidJumpThreshold: Double,
    val fluidAboveHead: Boolean,
    val depthStrider: Double,
    val dolphinsGrace: Boolean,
    val swimming: Boolean,
    val climbing: Boolean,
    val scaffolding: Boolean,
    val stuckMultiplier: Vec?,
    val cobweb: Boolean,
    val tolerance: Double,
) {
    val slowed get() = blockSpeedFactor != 1.0
}
