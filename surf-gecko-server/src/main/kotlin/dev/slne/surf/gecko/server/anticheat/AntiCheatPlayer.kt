package dev.slne.surf.gecko.server.anticheat

import dev.slne.surf.gecko.server.anticheat.check.BlockProbe
import dev.slne.surf.gecko.server.anticheat.check.CheckType
import dev.slne.surf.gecko.server.anticheat.check.PositionHistory
import dev.slne.surf.gecko.server.anticheat.check.ReachCheck
import dev.slne.surf.gecko.server.anticheat.check.TimerCheck
import dev.slne.surf.gecko.server.anticheat.check.Violation
import dev.slne.surf.gecko.server.anticheat.check.ViolationTracker
import dev.slne.surf.gecko.server.anticheat.simulation.MovementEnvironment
import dev.slne.surf.gecko.server.anticheat.simulation.MovementExemption
import dev.slne.surf.gecko.server.anticheat.simulation.MovementInput
import dev.slne.surf.gecko.server.anticheat.simulation.MovementSimulator
import dev.slne.surf.gecko.server.anticheat.simulation.SimulationContext
import dev.slne.surf.gecko.server.anticheat.simulation.SimulationState
import dev.slne.surf.gecko.server.config.Config
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.util.Locale
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

private const val GROUND_SUPPORT_DEPTH = 0.03
private const val PHASE_SHRINK = 0.02
private const val PUSH_RANGE = 2.0
private const val PUSH_MARGIN = 0.1
private const val BUDGET_DEADZONE_DIVISOR = 3.0
private const val INVENTORY_GRACE_TICKS = 2
private const val STEP_EPSILON = 1.0E-4

class AntiCheatPlayer(
    val player: Player,
    private val settings: Config.AntiCheatConfig,
    private val report: (Player, Violation) -> Unit,
) {
    val violations = ViolationTracker(settings)
    val positions = PositionHistory()

    private val timer = TimerCheck(settings.maxTimerDriftMillis)

    private var baseline: SimulationState? = null
    private var lastInstance: Instance? = null
    private var lastPosition: Pos? = null
    private var lastLegalPosition: Pos? = null
    private var lastGroundPosition: Pos? = null
    private var packetPosition: Pos? = null
    private var claimedGround = false
    private var previousClaimedGround = false
    private var previousSupported = false
    private var previousUncertain = false
    private var previousUsingItem = false
    private var lastSupported = false
    private var moved = false
    private var graceTicks = settings.joinGraceTicks
    private var graceReason: MovementExemption? = MovementExemption.JOINING
    private var groundSpoofStreak = 0
    private var phaseStreak = 0
    private var phaseBlock: Long? = null
    private var inventoryTicks = 0
    private var horizontalBudget = 0.0
    private var verticalBudget = 0.0
    private var flaggedThisTick = false
    private var mitigationRequested = false
    private var lastSetbackAt = 0L

    @Volatile
    var snapshot: AntiCheatSnapshot = AntiCheatSnapshot.EMPTY
        private set

    fun onMove(position: Pos, onGround: Boolean) {
        packetPosition = position
        claimedGround = onGround
        moved = true
    }

    fun onGroundStatus(onGround: Boolean) {
        claimedGround = onGround
    }

    fun onTeleport() {
        baseline = null
        grace(MovementExemption.TELEPORT, settings.teleportGraceTicks)
    }

    fun onVelocity() {
        baseline = null
        grace(MovementExemption.KNOCKBACK, settings.knockbackGraceTicks)
    }

    fun onRespawn() {
        baseline = null
        timer.reset()
        positions.clear()
        grace(MovementExemption.JOINING, settings.joinGraceTicks)
    }

    fun tick() {
        violations.decay()
        flaggedThisTick = false
        mitigationRequested = false

        timer.record()?.let { drift ->
            flag(CheckType.TIMER, "%+d ms".format(Locale.ROOT, drift))
        }

        val reported = packetPosition ?: player.position
        positions.record(reported)

        val hadMovement = moved
        moved = false

        if (player.instance !== lastInstance) {
            lastInstance = player.instance
            baseline = null
            grace(MovementExemption.JOINING, settings.joinGraceTicks)
        }

        if (player.lastSentTeleportId != player.lastReceivedTeleportId) {
            grace(MovementExemption.TELEPORT, settings.teleportGraceTicks)
        }

        checkInventoryMove()

        val previous = baseline
        val environment = MovementEnvironment.of(
            player,
            previous?.position ?: reported,
            reported,
            settings
        )
        val context = environment.context
        val exemption = graceReason ?: environment.exemption

        if (exemption != null || context == null || previous == null) {
            consumeGrace()
            resetMovementState(reported)
            publish(0.0, 0.0, 0.0, reported, exemption, claimedGround)
            applyPendingSetback()
            return
        }

        val supported = BlockProbe.hasGroundSupport(
            reported,
            context.boundingBox,
            context.blockGetter,
            GROUND_SUPPORT_DEPTH
        )
        val prediction = predict(previous, context, reported)
        val stepCeiling = if (previous.onGround && supported) {
            previous.position.y() + MovementSimulator.STEP_HEIGHT
        } else {
            Double.NEGATIVE_INFINITY
        }
        val rise = reported.y() - previous.position.y()
        val stepped = previous.onGround && supported &&
                rise > STEP_EPSILON && rise <= MovementSimulator.STEP_HEIGHT
        val uncertain = prediction.state.collided || stepped ||
                prediction.state.onGround != previous.onGround
        val allowance = if (uncertain || previousUncertain) {
            max(prediction.maxHorizontal, MovementSimulator.maxHorizontalStep(previous, context))
        } else {
            prediction.maxHorizontal
        }
        val horizontalExcess = horizontalDistance(previous.position, reported) - allowance
        val verticalExcess = reported.y() - max(prediction.maxVertical, stepCeiling)

        if (hadMovement) {
            inspect(
                prediction,
                horizontalExcess,
                verticalExcess,
                reported,
                context,
                supported,
                uncertain || previousUncertain
            )
        }

        baseline = SimulationState(
            reported,
            if (uncertain) {
                MovementSimulator.observedVelocity(
                    previous,
                    context,
                    reported.x() - previous.position.x(),
                    reported.z() - previous.position.z(),
                    prediction.state.velocity
                )
            } else {
                prediction.state.velocity
            },
            prediction.state.onGround
        )
        previousUncertain = uncertain
        previousClaimedGround = claimedGround
        previousSupported = supported
        previousUsingItem = player.isUsingItem
        lastSupported = supported

        if (!flaggedThisTick) {
            lastLegalPosition = reported

            if (supported) {
                lastGroundPosition = reported
            }
        }

        publish(
            prediction.offset,
            horizontalExcess,
            verticalExcess,
            reported,
            null,
            prediction.state.onGround
        )
        applyPendingSetback()
    }

    fun validateAttack(target: Entity): Boolean {
        val eye = player.position.add(0.0, player.eyeHeight, 0.0)
        val window = System.currentTimeMillis() -
                player.latency - settings.reachLagCompensationMillis
        val candidates = historyOf(target, window)
        val distance = ReachCheck.closestDistance(eye, target.boundingBox, candidates)

        if (distance <= settings.maxReach) {
            return true
        }

        val violation = flag(CheckType.REACH, "%.2f Blöcke".format(Locale.ROOT, distance))

        return !settings.mitigate || violation.level < settings.mitigationThreshold
    }

    private fun historyOf(target: Entity, window: Long): List<Pos> {
        val tracked = if (target is Player) AntiCheatTracker.find(target) else null

        return tracked?.positions?.since(window) ?: listOf(target.position)
    }

    private fun resetMovementState(reported: Pos) {
        baseline = SimulationState(reported, baseline?.velocity ?: Vec.ZERO, claimedGround)
        previousClaimedGround = claimedGround
        previousSupported = true
        previousUncertain = false
        previousUsingItem = player.isUsingItem
        lastSupported = true
        groundSpoofStreak = 0
        phaseStreak = 0
        phaseBlock = null
        horizontalBudget = 0.0
        verticalBudget = 0.0

        if (!flaggedThisTick) {
            lastLegalPosition = reported

            if (claimedGround) {
                lastGroundPosition = reported
            }
        }
    }

    private fun inspect(
        prediction: Prediction,
        horizontalExcess: Double,
        verticalExcess: Double,
        reported: Pos,
        context: SimulationContext,
        supported: Boolean,
        uncertain: Boolean
    ) {
        if (checkPhase(reported, context)) {
            groundSpoofStreak = 0
            horizontalBudget = 0.0
            verticalBudget = 0.0
            return
        }

        checkGroundSupport(context, supported)

        val tolerance = context.tolerance

        if (uncertain) {
            horizontalBudget = 0.0
            verticalBudget = 0.0
        } else {
            val deadzone = tolerance / BUDGET_DEADZONE_DIVISOR

            horizontalBudget = (horizontalBudget + horizontalExcess - deadzone).coerceAtLeast(0.0)
            verticalBudget = (verticalBudget + verticalExcess - deadzone).coerceAtLeast(0.0)
        }

        val type: CheckType?
        val amount: Double

        when {
            !uncertain && verticalExcess > tolerance && verticalExcess >= horizontalExcess -> {
                type = verticalCheck(context)
                amount = verticalExcess
            }

            horizontalExcess > tolerance -> {
                type = horizontalCheck(context)
                amount = horizontalExcess
            }

            verticalBudget > settings.excessBudget -> {
                type = verticalCheck(context)
                amount = verticalBudget
            }

            horizontalBudget > settings.excessBudget -> {
                type = horizontalCheck(context)
                amount = horizontalBudget
            }

            !uncertain && prediction.offset > settings.maxDesyncOffset -> {
                type = CheckType.PREDICTION
                amount = prediction.offset
            }

            else -> {
                type = null
                amount = 0.0
            }
        }

        if (type != null && !pushedByEntity(reported)) {
            flag(type, "diff %.4f".format(Locale.ROOT, amount))
            horizontalBudget = 0.0
            verticalBudget = 0.0
        }
    }

    private fun horizontalCheck(context: SimulationContext) = when {
        context.cobweb -> CheckType.NO_WEB
        context.slowed || player.isUsingItem || player.isSneaking -> CheckType.NO_SLOW
        else -> CheckType.SPEED
    }

    private fun verticalCheck(context: SimulationContext) =
        if (context.cobweb) CheckType.NO_WEB else CheckType.FLIGHT

    private fun pushedByEntity(position: Pos): Boolean {
        val instance = player.instance ?: return false
        val box = player.boundingBox.growSymmetrically(PUSH_MARGIN, PUSH_MARGIN, PUSH_MARGIN)

        return instance.getNearbyEntities(position, PUSH_RANGE).any { other ->
            other !== player && box.intersectBox(position.sub(other.position), other.boundingBox)
        }
    }

    private fun checkInventoryMove() {
        if (player.openInventory == null) {
            inventoryTicks = 0
            return
        }

        inventoryTicks++

        if (inventoryTicks <= INVENTORY_GRACE_TICKS) {
            return
        }

        val inputs = player.inputs()
        val pressed = inputs.forward() || inputs.backward() || inputs.left() ||
                inputs.right() || inputs.jump() || inputs.sprint()

        if (pressed) {
            flag(CheckType.INVENTORY_MOVE, "Eingabe bei offenem Inventar")
        }
    }

    private fun checkGroundSupport(context: SimulationContext, supported: Boolean) {
        if (context.inWater || context.climbing) {
            groundSpoofStreak = 0
            return
        }

        if (claimedGround && !supported) {
            groundSpoofStreak++

            if (groundSpoofStreak >= settings.groundSpoofTicks) {
                flag(CheckType.GROUND_SPOOF, "$groundSpoofStreak Ticks ohne Boden")
            }
        } else {
            groundSpoofStreak = 0
        }
    }

    private fun checkPhase(reported: Pos, context: SimulationContext): Boolean {
        val inside = BlockProbe.intersectsSolid(
            reported,
            BlockProbe.shrink(context.boundingBox, PHASE_SHRINK),
            context.blockGetter
        )

        if (!inside) {
            phaseStreak = 0
            phaseBlock = null
            return false
        }

        val block = blockKey(reported)
        val previousBlock = phaseBlock

        phaseBlock = block

        if (previousBlock == null || previousBlock == block) {
            return true
        }

        phaseStreak++

        if (phaseStreak >= settings.phaseTicks) {
            flag(CheckType.PHASE, "$phaseStreak Blöcke durchquert")
        }

        return true
    }

    private fun blockKey(position: Pos): Long {
        val x = floor(position.x()).toLong() and BLOCK_MASK
        val y = floor(position.y()).toLong() and BLOCK_MASK
        val z = floor(position.z()).toLong() and BLOCK_MASK

        return (x shl 42) or (y shl 21) or z
    }

    private fun predict(
        previous: SimulationState,
        context: SimulationContext,
        target: Pos
    ): Prediction {
        val input = MovementInput.of(player)
        var best = previous
        var bestOffset = Double.MAX_VALUE
        var maxHorizontal = 0.0
        var maxVertical = Double.NEGATIVE_INFINITY

        for (onGround in groundStates(previous)) {
            val from = previous.copy(onGround = onGround)

            for (candidate in candidates(input)) {
                val simulated = MovementSimulator.simulate(from, candidate, context)
                val offset = simulated.position.distance(target)

                if (offset < bestOffset) {
                    bestOffset = offset
                    best = simulated
                }

                val horizontal = horizontalDistance(previous.position, simulated.position)

                if (horizontal > maxHorizontal) {
                    maxHorizontal = horizontal
                }

                if (simulated.position.y() > maxVertical) {
                    maxVertical = simulated.position.y()
                }
            }
        }

        return Prediction(best, bestOffset, maxHorizontal, maxVertical)
    }

    private fun groundStates(previous: SimulationState) = when {
        previous.onGround == previousClaimedGround -> single(previous.onGround)
        !previousSupported -> single(previous.onGround)
        else -> BOTH
    }

    private fun candidates(input: MovementInput): List<MovementInput> {
        val jumps = if (input.jump) BOTH else ONLY_FALSE
        val items = if (input.usingItem == previousUsingItem) {
            single(input.usingItem)
        } else {
            BOTH
        }
        val result = ArrayList<MovementInput>(BOTH.size * jumps.size * items.size)

        for (sprint in BOTH) {
            for (jump in jumps) {
                for (usingItem in items) {
                    result.add(input.copy(sprint = sprint, jump = jump, usingItem = usingItem))
                }
            }
        }

        return result
    }

    private fun flag(type: CheckType, detail: String): Violation {
        val violation = violations.flag(type, detail)

        report(player, violation)

        if (type.mitigable) {
            flaggedThisTick = true

            if (settings.mitigate && violation.level >= settings.mitigationThreshold) {
                mitigationRequested = true
            }
        }

        return violation
    }

    private fun applyPendingSetback() {
        if (!mitigationRequested) {
            return
        }

        val anchor = if (lastSupported) {
            lastLegalPosition
        } else {
            lastGroundPosition ?: lastLegalPosition
        }
        val target = anchor ?: return
        val now = System.currentTimeMillis()

        if (now - lastSetbackAt < settings.mitigationCooldownMillis) {
            return
        }

        val current = player.position

        lastSetbackAt = now
        baseline = null
        packetPosition = null
        lastPosition = target
        horizontalBudget = 0.0
        verticalBudget = 0.0
        groundSpoofStreak = 0
        phaseStreak = 0
        phaseBlock = null
        violations.clearMitigable()
        grace(MovementExemption.TELEPORT, settings.teleportGraceTicks)
        player.velocity = Vec.ZERO
        player.teleport(target.withView(current.yaw(), current.pitch()))
    }

    private fun grace(reason: MovementExemption, ticks: Int) {
        if (graceReason != null && ticks <= graceTicks) {
            return
        }

        graceTicks = ticks
        graceReason = reason
    }

    private fun consumeGrace() {
        if (graceTicks > 0) {
            graceTicks--
        }

        if (graceTicks == 0) {
            graceReason = null
        }
    }

    private fun publish(
        offset: Double,
        horizontalExcess: Double,
        verticalExcess: Double,
        reported: Pos,
        exemption: MovementExemption?,
        simulatedGround: Boolean
    ) {
        val previousPosition = lastPosition
        lastPosition = reported

        snapshot = AntiCheatSnapshot(
            offset = offset,
            horizontalExcess = horizontalExcess,
            verticalExcess = verticalExcess,
            speed = if (previousPosition == null) {
                0.0
            } else {
                horizontalDistance(previousPosition, reported)
            },
            claimedGround = claimedGround,
            simulatedGround = simulatedGround,
            exemption = exemption,
            violationLevel = violations.total(),
        )
    }

    private fun horizontalDistance(from: Pos, to: Pos) =
        hypot(to.x() - from.x(), to.z() - from.z())

    private fun single(value: Boolean) = if (value) ONLY_TRUE else ONLY_FALSE

    private class Prediction(
        val state: SimulationState,
        val offset: Double,
        val maxHorizontal: Double,
        val maxVertical: Double,
    )

    private companion object {
        const val BLOCK_MASK = 0x1FFFFFL

        val BOTH = listOf(true, false)
        val ONLY_TRUE = listOf(true)
        val ONLY_FALSE = listOf(false)
    }
}
