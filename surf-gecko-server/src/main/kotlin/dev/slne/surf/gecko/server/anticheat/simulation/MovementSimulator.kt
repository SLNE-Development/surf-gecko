package dev.slne.surf.gecko.server.anticheat.simulation

import dev.slne.surf.gecko.server.anticheat.check.BlockProbe
import net.minestom.server.collision.CollisionUtils
import net.minestom.server.collision.PhysicsResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val INPUT_DAMPING = 0.98
private const val SNEAK_FACTOR = 0.3
private const val ITEM_USE_FACTOR = 0.2
private const val SPRINT_JUMP_IMPULSE = 0.2
private const val SPRINT_SPEED_MULTIPLIER = 1.3
private const val GROUND_SPEED_BASE = 0.21600002
private const val AIR_SPEED = 0.02
private const val AIR_SPRINT_SPEED = 0.025999999
private const val AIR_DRAG = 0.91
private const val VERTICAL_DRAG = 0.98
private const val FLY_SPRINT_MULTIPLIER = 2.0
private const val FLY_VERTICAL_IMPULSE = 3.0
private const val FLY_VERTICAL_DRAG = 0.6
private const val MIN_INPUT_LENGTH_SQUARED = 1.0E-7
private const val COLLISION_EPSILON = 1.0E-7

private const val WATER_SPEED = 0.02
private const val WATER_FRICTION = 0.8
private const val WATER_SPRINT_FRICTION = 0.9
private const val WATER_STRIDER_FRICTION = 0.54600006
private const val WATER_VERTICAL_DRAG = 0.8
private const val WATER_SINK_DIVISOR = 16.0
private const val WATER_JUMP_IMPULSE = 0.04
private const val WATER_EDGE_JUMP = 0.3
private const val WATER_EDGE_PROBE = 0.6
private const val DOLPHINS_GRACE_FRICTION = 0.96
private const val SWIM_LOOK_STEEP = -0.2
private const val SWIM_FACTOR_STEEP = 0.085
private const val SWIM_FACTOR_FLAT = 0.06
private const val SINK_SNAP = 0.003
private const val SINK_SNAP_REFERENCE = 0.005

private const val LANDING_PROBE = 0.2
private const val SLIME_STEP_LIMIT = 0.1
private const val SLIME_STEP_BASE = 0.4
private const val SLIME_STEP_SCALE = 0.2

private const val CLIMB_SPEED = 0.15
private const val CLIMB_ASCEND = 0.2

object MovementSimulator {

    const val STEP_HEIGHT = 0.6

    fun simulate(
        state: SimulationState,
        input: MovementInput,
        context: SimulationContext
    ): SimulationState {
        val velocity = applyJump(state, input, context)

        return when {
            context.flying -> travelFlying(state, input, context, velocity)
            context.inWater -> travelWater(state, input, context, velocity)
            else -> travelAir(state, input, context, velocity)
        }
    }

    fun maxHorizontalStep(state: SimulationState, context: SimulationContext): Double {
        val speed = hypot(state.velocity.x(), state.velocity.z())
        val acceleration = when {
            context.flying -> context.flyingSpeed * FLY_SPRINT_MULTIPLIER
            context.inWater -> maxWaterAcceleration(context)

            state.onGround -> context.movementSpeed * SPRINT_SPEED_MULTIPLIER *
                    (GROUND_SPEED_BASE / (context.blockFriction * context.blockFriction * context.blockFriction))

            else -> AIR_SPRINT_SPEED
        }
        val impulse = if (state.onGround && !context.inWater) SPRINT_JUMP_IMPULSE else 0.0
        val stuck = context.stuckMultiplier
        val stuckFactor = if (stuck == null) 1.0 else max(stuck.x(), stuck.z())

        return (speed + acceleration + impulse) * stuckFactor
    }

    fun observedVelocity(
        previous: SimulationState,
        context: SimulationContext,
        observedX: Double,
        observedZ: Double,
        simulated: Vec
    ): Vec {
        if (context.stuckMultiplier != null) {
            return Vec(0.0, simulated.y(), 0.0)
        }

        val bound = maxHorizontalStep(previous, context)
        val length = hypot(observedX, observedZ)
        val clamp = if (length > bound && length > 0.0) bound / length else 1.0
        val drag = horizontalDrag(previous, context) * context.blockSpeedFactor

        return Vec(observedX * clamp * drag, simulated.y(), observedZ * clamp * drag)
    }

    private fun horizontalDrag(state: SimulationState, context: SimulationContext) = when {
        context.inWater -> WATER_SPRINT_FRICTION
        state.onGround && !context.flying -> context.blockFriction * AIR_DRAG
        else -> AIR_DRAG
    }

    private fun maxWaterAcceleration(context: SimulationContext): Double {
        if (context.depthStrider <= 0.0) {
            return WATER_SPEED
        }

        return WATER_SPEED + (context.movementSpeed - WATER_SPEED) * context.depthStrider / 3.0
    }

    private fun applyJump(
        state: SimulationState,
        input: MovementInput,
        context: SimulationContext
    ): Vec {
        val velocity = state.velocity

        if (context.flying) {
            val impulse = context.flyingSpeed * FLY_VERTICAL_IMPULSE
            var vertical = velocity.y()

            if (input.jump) vertical += impulse
            if (input.sneak) vertical -= impulse

            return velocity.withY(vertical)
        }

        if (!input.jump) {
            return velocity
        }

        val shallow = state.onGround && context.fluidHeight <= context.fluidJumpThreshold

        if (context.inWater && !shallow) {
            return velocity.add(0.0, WATER_JUMP_IMPULSE, 0.0)
        }

        if (!state.onGround && !shallow) {
            return velocity
        }

        val jumped = velocity.withY(context.jumpPower)

        if (!input.sprint) {
            return jumped
        }

        val yaw = Math.toRadians(context.yaw.toDouble())

        return jumped.add(
            -sin(yaw) * SPRINT_JUMP_IMPULSE,
            0.0,
            cos(yaw) * SPRINT_JUMP_IMPULSE
        )
    }

    private fun travelAir(
        state: SimulationState,
        input: MovementInput,
        context: SimulationContext,
        initial: Vec
    ): SimulationState {
        val friction = if (state.onGround) context.blockFriction else 1.0
        val drag = friction * AIR_DRAG
        val acceleration = when {
            state.onGround -> {
                val sprintFactor = if (input.sprint) SPRINT_SPEED_MULTIPLIER else 1.0

                context.movementSpeed * sprintFactor *
                        (GROUND_SPEED_BASE / (friction * friction * friction))
            }

            input.sprint -> AIR_SPRINT_SPEED
            else -> AIR_SPEED
        }

        var velocity = accelerate(initial, input, acceleration, context.yaw)

        if (context.climbing) {
            velocity = clampClimb(velocity, input, context)
        }

        val outcome = move(state, velocity, input, context, stepping = true)
        var displacement = outcome.displacement

        if (context.climbing && (outcome.horizontalCollision || input.jump)) {
            displacement = displacement.withY(CLIMB_ASCEND)
        }

        val vertical = (displacement.y() - context.gravity) * VERTICAL_DRAG

        return SimulationState(
            outcome.position,
            Vec(displacement.x() * drag, vertical, displacement.z() * drag),
            outcome.onGround,
            outcome.blocked
        )
    }

    private fun travelFlying(
        state: SimulationState,
        input: MovementInput,
        context: SimulationContext,
        initial: Vec
    ): SimulationState {
        val acceleration = context.flyingSpeed * if (input.sprint) FLY_SPRINT_MULTIPLIER else 1.0
        val velocity = accelerate(initial, input, acceleration, context.yaw)
        val outcome = move(state, velocity, input, context, stepping = false)

        return SimulationState(
            outcome.position,
            Vec(
                outcome.displacement.x() * AIR_DRAG,
                initial.y() * FLY_VERTICAL_DRAG,
                outcome.displacement.z() * AIR_DRAG
            ),
            outcome.onGround,
            outcome.blocked
        )
    }

    private fun travelWater(
        state: SimulationState,
        input: MovementInput,
        context: SimulationContext,
        initial: Vec
    ): SimulationState {
        var velocity = initial

        if (context.swimming && swimAdjusts(input, context)) {
            velocity = velocity.withY(
                velocity.y() + (context.lookY - velocity.y()) * swimFactor(context)
            )
        }

        val falling = velocity.y() <= 0.0
        val strider = if (state.onGround) context.depthStrider else context.depthStrider / 2.0
        var friction = if (input.sprint) WATER_SPRINT_FRICTION else WATER_FRICTION
        var acceleration = WATER_SPEED

        if (strider > 0.0) {
            friction += (WATER_STRIDER_FRICTION - friction) * strider / 3.0
            acceleration += (context.movementSpeed - acceleration) * strider / 3.0
        }

        if (context.dolphinsGrace) {
            friction = DOLPHINS_GRACE_FRICTION
        }

        velocity = accelerate(velocity, input, acceleration, context.yaw)

        if (context.climbing) {
            velocity = clampClimb(velocity, input, context)
        }

        val outcome = move(state, velocity, input, context, stepping = true)
        var displacement = outcome.displacement

        if (context.climbing && outcome.horizontalCollision) {
            displacement = displacement.withY(CLIMB_ASCEND)
        }

        displacement = Vec(
            displacement.x() * friction,
            displacement.y() * WATER_VERTICAL_DRAG,
            displacement.z() * friction
        )
        displacement = sink(displacement, context.gravity, falling, input.sprint)

        if (outcome.horizontalCollision && escapesWater(state, outcome, displacement, context)) {
            displacement = displacement.withY(WATER_EDGE_JUMP)
        }

        return SimulationState(
            outcome.position,
            displacement,
            outcome.onGround,
            outcome.blocked
        )
    }

    private fun swimAdjusts(input: MovementInput, context: SimulationContext) =
        context.lookY <= 0.0 || input.jump || context.fluidAboveHead

    private fun swimFactor(context: SimulationContext) =
        if (context.lookY < SWIM_LOOK_STEEP) SWIM_FACTOR_STEEP else SWIM_FACTOR_FLAT

    private fun sink(
        displacement: Vec,
        gravity: Double,
        falling: Boolean,
        sprinting: Boolean
    ): Vec {
        if (gravity == 0.0 || sprinting) {
            return displacement
        }

        val step = gravity / WATER_SINK_DIVISOR
        val current = displacement.y()
        val snapped = falling &&
                abs(current - SINK_SNAP_REFERENCE) >= SINK_SNAP &&
                abs(current - step) < SINK_SNAP

        return displacement.withY(if (snapped) -SINK_SNAP else current - step)
    }

    private fun escapesWater(
        state: SimulationState,
        outcome: MoveOutcome,
        displacement: Vec,
        context: SimulationContext
    ): Boolean {
        val vertical = displacement.y() + WATER_EDGE_PROBE -
                outcome.position.y() + state.position.y()
        val probe = outcome.position.add(displacement.x(), vertical, displacement.z())

        return !BlockProbe.intersectsSolid(probe, context.boundingBox, context.blockGetter)
    }

    private fun clampClimb(
        velocity: Vec,
        input: MovementInput,
        context: SimulationContext
    ): Vec {
        var vertical = velocity.y().coerceAtLeast(-CLIMB_SPEED)

        if (vertical < 0.0 && input.sneak && !context.scaffolding) {
            vertical = 0.0
        }

        return Vec(
            velocity.x().coerceIn(-CLIMB_SPEED, CLIMB_SPEED),
            vertical,
            velocity.z().coerceIn(-CLIMB_SPEED, CLIMB_SPEED)
        )
    }

    private fun accelerate(
        velocity: Vec,
        input: MovementInput,
        speed: Double,
        yaw: Float
    ): Vec {
        val relative = rotatedInput(input, speed, Math.toRadians(yaw.toDouble()))

        return velocity.add(relative.x(), relative.y(), relative.z())
    }

    private fun rotatedInput(input: MovementInput, speed: Double, yaw: Double): Vec {
        var forward = (if (input.forward) 1.0 else 0.0) - (if (input.backward) 1.0 else 0.0)
        var strafe = (if (input.left) 1.0 else 0.0) - (if (input.right) 1.0 else 0.0)

        if (input.sneak) {
            forward *= SNEAK_FACTOR
            strafe *= SNEAK_FACTOR
        }

        if (input.usingItem) {
            forward *= ITEM_USE_FACTOR
            strafe *= ITEM_USE_FACTOR
        }

        forward *= INPUT_DAMPING
        strafe *= INPUT_DAMPING

        val lengthSquared = forward * forward + strafe * strafe

        if (lengthSquared < MIN_INPUT_LENGTH_SQUARED) {
            return Vec.ZERO
        }

        val scale = speed / if (lengthSquared > 1.0) sqrt(lengthSquared) else 1.0
        val x = strafe * scale
        val z = forward * scale
        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)

        return Vec(x * cosYaw - z * sinYaw, 0.0, z * cosYaw + x * sinYaw)
    }

    private fun move(
        state: SimulationState,
        velocity: Vec,
        input: MovementInput,
        context: SimulationContext,
        stepping: Boolean
    ): MoveOutcome {
        val stuck = context.stuckMultiplier
        val requested = if (stuck == null) {
            velocity
        } else {
            Vec(velocity.x() * stuck.x(), velocity.y() * stuck.y(), velocity.z() * stuck.z())
        }

        val collision = collide(context, state.position, requested)

        var position = collision.newPosition()
        var displacement = collision.newVelocity()
        var onGround = collision.isOnGround
        var didStep = false

        if (stepping && state.onGround && (collision.collisionX() || collision.collisionZ())) {
            val stepped = step(context, state.position, requested, position)

            if (stepped != null) {
                displacement = Vec(
                    stepped.x() - state.position.x(),
                    0.0,
                    stepped.z() - state.position.z()
                )
                position = stepped
                onGround = requested.y() < 0.0
                didStep = true
            }
        }

        val horizontalCollision =
            abs(displacement.x() - requested.x()) > COLLISION_EPSILON ||
                    abs(displacement.z() - requested.z()) > COLLISION_EPSILON

        displacement = applyBounce(
            context,
            input,
            position,
            requested,
            displacement,
            collision.collisionY(),
            onGround
        )

        if (stuck != null) {
            displacement = Vec.ZERO
        }

        if (context.blockSpeedFactor != 1.0) {
            displacement = Vec(
                displacement.x() * context.blockSpeedFactor,
                displacement.y(),
                displacement.z() * context.blockSpeedFactor
            )
        }

        return MoveOutcome(
            position,
            displacement,
            onGround,
            horizontalCollision,
            horizontalCollision || didStep
        )
    }

    private fun applyBounce(
        context: SimulationContext,
        input: MovementInput,
        position: Pos,
        requested: Vec,
        displacement: Vec,
        verticalCollision: Boolean,
        onGround: Boolean
    ): Vec {
        val landing = verticalCollision && requested.y() < 0.0

        if (input.sneak || !landing && !onGround) {
            return displacement
        }

        val below = context.blockGetter.getBlock(position.sub(0.0, LANDING_PROBE, 0.0))
        var result = displacement

        if (landing) {
            val bounce = BounceBlocks.bounceOf(below)

            if (bounce > 0.0) {
                result = result.withY(-requested.y() * bounce)
            }
        }

        if (onGround && BounceBlocks.isSlime(below)) {
            val vertical = abs(requested.y())

            if (vertical < SLIME_STEP_LIMIT) {
                val factor = SLIME_STEP_BASE + vertical * SLIME_STEP_SCALE

                result = Vec(result.x() * factor, result.y(), result.z() * factor)
            }
        }

        return result
    }

    private fun step(
        context: SimulationContext,
        origin: Pos,
        velocity: Vec,
        direct: Pos
    ): Pos? {
        val raised = collide(context, origin, Vec(0.0, STEP_HEIGHT, 0.0)).newPosition()
        val advanced =
            collide(context, raised, Vec(velocity.x(), 0.0, velocity.z())).newPosition()
        val descent = origin.y() - advanced.y() + velocity.y()
        val lowered = collide(context, advanced, Vec(0.0, descent, 0.0)).newPosition()

        if (horizontalDistanceSquared(lowered, origin) <= horizontalDistanceSquared(direct, origin)) {
            return null
        }

        return lowered
    }

    private fun collide(context: SimulationContext, position: Pos, velocity: Vec): PhysicsResult =
        CollisionUtils.handlePhysics(
            context.blockGetter,
            context.worldBorder,
            context.boundingBox,
            position,
            velocity,
            null,
            false
        )

    private fun horizontalDistanceSquared(from: Pos, to: Pos): Double {
        val deltaX = from.x() - to.x()
        val deltaZ = from.z() - to.z()

        return deltaX * deltaX + deltaZ * deltaZ
    }

    private class MoveOutcome(
        val position: Pos,
        val displacement: Vec,
        val onGround: Boolean,
        val horizontalCollision: Boolean,
        val blocked: Boolean,
    )
}
