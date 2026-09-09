package dev.slne.surf.gecko.server.anticheat.simulation

import dev.slne.surf.gecko.server.combat.enchantmentLevel
import dev.slne.surf.gecko.server.config.Config
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.EntityPose
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.instance.block.Block
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.potion.PotionEffect
import net.minestom.server.utils.chunk.ChunkCache
import kotlin.math.floor
import kotlin.math.max

private const val GROUND_PROBE_DEPTH = 0.5000001
private const val DEFAULT_GRAVITY = 0.08
private const val SLOW_FALLING_GRAVITY = 0.01
private const val BASE_JUMP_POWER = 0.42
private const val JUMP_BOOST_PER_LEVEL = 0.1
private const val SPEED_EFFECT_PER_LEVEL = 0.2
private const val SLOWNESS_EFFECT_PER_LEVEL = 0.15
private const val FLUID_JUMP_THRESHOLD = 0.4
private const val MAX_DEPTH_STRIDER = 3.0
private const val HEAD_FLUID_OFFSET = 0.9
private const val FLUID_LEVELS = 9.0
private const val FLUID_MAX_AMOUNT = 8

private val CLIMBABLE_BLOCKS = setOf(
    Block.LADDER.key(),
    Block.VINE.key(),
    Block.SCAFFOLDING.key(),
    Block.TWISTING_VINES.key(),
    Block.TWISTING_VINES_PLANT.key(),
    Block.WEEPING_VINES.key(),
    Block.WEEPING_VINES_PLANT.key(),
    Block.CAVE_VINES.key(),
    Block.CAVE_VINES_PLANT.key(),
)

private val SPECIAL_BLOCKS = setOf(
    Block.POWDER_SNOW.key(),
    Block.BUBBLE_COLUMN.key(),
    Block.BIG_DRIPLEAF.key(),
    Block.HONEY_BLOCK.key(),
)

private val COBWEB_MULTIPLIER = Vec(0.25, 0.05, 0.25)
private val BERRY_BUSH_MULTIPLIER = Vec(0.8, 0.75, 0.8)

class MovementEnvironment private constructor(
    val context: SimulationContext?,
    val exemption: MovementExemption?,
) {
    companion object {
        fun of(
            player: Player,
            position: Pos,
            view: Pos,
            settings: Config.AntiCheatConfig
        ): MovementEnvironment {
            playerExemption(player)?.let { return MovementEnvironment(null, it) }

            val instance = player.instance
                ?: return MovementEnvironment(null, MovementExemption.NO_INSTANCE)
            val chunk = instance.getChunkAt(position)

            val destination = instance.getChunkAt(view)

            if (chunk == null || !chunk.isLoaded || destination == null || !destination.isLoaded) {
                return MovementEnvironment(null, MovementExemption.UNLOADED_CHUNK)
            }

            val blockGetter = ChunkCache(instance, chunk, Block.STONE)
            val boundingBox = player.boundingBox
            val scan = scanBlocks(player, position, blockGetter)

            scan.exemption?.let { return MovementEnvironment(null, it) }

            val below = blockGetter.getBlock(position.sub(0.0, GROUND_PROBE_DEPTH, 0.0))
            val gravity = if (player.hasEffect(PotionEffect.SLOW_FALLING)) {
                SLOW_FALLING_GRAVITY
            } else {
                DEFAULT_GRAVITY
            }

            val fluidHeight = max(0.0, scan.waterTop - (position.y() + boundingBox.minY()))
            val inWater = fluidHeight > 0.0

            val context = SimulationContext(
                blockGetter = blockGetter,
                worldBorder = instance.worldBorder,
                boundingBox = boundingBox,
                yaw = view.yaw(),
                lookY = view.direction().y(),
                gravity = gravity,
                movementSpeed = movementSpeed(player),
                flyingSpeed = player.flyingSpeed.toDouble(),
                blockFriction = below.friction().toDouble(),
                blockSpeedFactor = speedFactor(scan.feet, below),
                jumpPower = jumpPower(player, below),
                flying = player.isFlying,
                inWater = inWater,
                fluidHeight = fluidHeight,
                fluidJumpThreshold = fluidJumpThreshold(player),
                fluidAboveHead = blockGetter
                    .getBlock(position.add(0.0, HEAD_FLUID_OFFSET, 0.0))
                    .fluid(),
                depthStrider = depthStrider(player),
                dolphinsGrace = player.hasEffect(PotionEffect.DOLPHINS_GRACE),
                swimming = player.pose == EntityPose.SWIMMING,
                climbing = scan.climbing,
                scaffolding = scan.scaffolding,
                stuckMultiplier = scan.stuckMultiplier,
                cobweb = scan.cobweb,
                tolerance = tolerance(settings, inWater, scan.climbing, scan.stuckMultiplier != null),
            )

            return MovementEnvironment(context, null)
        }

        private fun tolerance(
            settings: Config.AntiCheatConfig,
            inWater: Boolean,
            climbing: Boolean,
            stuck: Boolean
        ) = when {
            inWater || stuck -> settings.fluidOffset
            climbing -> settings.climbOffset
            else -> settings.maxOffset
        }

        private fun playerExemption(player: Player) = when {
            player.gameMode == GameMode.SPECTATOR -> MovementExemption.GAME_MODE
            player.vehicle != null -> MovementExemption.VEHICLE
            player.isFlyingWithElytra -> MovementExemption.ELYTRA
            player.hasEffect(PotionEffect.LEVITATION) -> MovementExemption.LEVITATION
            else -> null
        }

        private fun scanBlocks(player: Player, position: Pos, blockGetter: Block.Getter): Scan {
            val boundingBox = player.boundingBox
            val minX = floor(position.x() + boundingBox.minX()).toInt()
            val maxX = floor(position.x() + boundingBox.maxX()).toInt()
            val minY = floor(position.y() + boundingBox.minY() - GROUND_PROBE_DEPTH).toInt()
            val insideMinY = floor(position.y() + boundingBox.minY()).toInt()
            val maxY = floor(position.y() + boundingBox.maxY()).toInt()
            val minZ = floor(position.z() + boundingBox.minZ()).toInt()
            val maxZ = floor(position.z() + boundingBox.maxZ()).toInt()

            val feetX = floor(position.x()).toInt()
            val feetY = floor(position.y()).toInt()
            val feetZ = floor(position.z()).toInt()
            val feet = blockGetter.getBlock(feetX, feetY, feetZ)
            val feetKey = feet.key()

            var waterTop = Double.NEGATIVE_INFINITY
            var stuckMultiplier: Vec? = null
            var cobweb = false

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        val block = blockGetter.getBlock(x, y, z)
                        val key = block.key()

                        if (key == Block.LAVA.key()) {
                            return Scan(exemption = MovementExemption.LAVA)
                        }

                        if (key in SPECIAL_BLOCKS) {
                            return Scan(exemption = MovementExemption.SPECIAL_BLOCK)
                        }

                        if (y >= insideMinY) {
                            if (key == Block.COBWEB.key()) {
                                cobweb = true
                                stuckMultiplier = COBWEB_MULTIPLIER
                            } else if (key == Block.SWEET_BERRY_BUSH.key() && stuckMultiplier == null) {
                                stuckMultiplier = BERRY_BUSH_MULTIPLIER
                            }
                        }

                        if (block.fluid()) {
                            val above = blockGetter.getBlock(x, y + 1, z)
                            val top = y + fluidHeightOf(block, above)

                            if (top > waterTop) {
                                waterTop = top
                            }
                        }
                    }
                }
            }

            return Scan(
                waterTop = waterTop,
                climbing = feetKey in CLIMBABLE_BLOCKS,
                scaffolding = feetKey == Block.SCAFFOLDING.key(),
                stuckMultiplier = stuckMultiplier,
                cobweb = cobweb,
                feet = feet,
            )
        }

        private fun fluidHeightOf(block: Block, above: Block): Double {
            if (above.fluid()) {
                return 1.0
            }

            val level = block.getProperty("level")?.toIntOrNull() ?: 0
            val amount = if (level >= FLUID_MAX_AMOUNT) FLUID_MAX_AMOUNT else FLUID_MAX_AMOUNT - level

            return amount / FLUID_LEVELS
        }

        private fun speedFactor(feet: Block?, below: Block): Double {
            val atFeet = (feet ?: below).speedFactor().toDouble()

            return if (atFeet == 1.0) below.speedFactor().toDouble() else atFeet
        }

        private fun fluidJumpThreshold(player: Player) =
            if (player.eyeHeight < FLUID_JUMP_THRESHOLD) 0.0 else FLUID_JUMP_THRESHOLD

        private fun depthStrider(player: Player) = player
            .getEquipment(EquipmentSlot.BOOTS)
            .enchantmentLevel(Enchantment.DEPTH_STRIDER)
            .toDouble()
            .coerceAtMost(MAX_DEPTH_STRIDER)

        private fun movementSpeed(player: Player): Double {
            var speed = player.getAttributeValue(Attribute.MOVEMENT_SPEED)
            val speedAmplifier = player.getEffectLevel(PotionEffect.SPEED)
            val slownessAmplifier = player.getEffectLevel(PotionEffect.SLOWNESS)

            if (speedAmplifier >= 0) {
                speed *= 1.0 + SPEED_EFFECT_PER_LEVEL * (speedAmplifier + 1)
            }

            if (slownessAmplifier >= 0) {
                speed *= max(0.0, 1.0 - SLOWNESS_EFFECT_PER_LEVEL * (slownessAmplifier + 1))
            }

            return speed
        }

        private fun jumpPower(player: Player, below: Block): Double {
            val jumpBoost = player.getEffectLevel(PotionEffect.JUMP_BOOST)
            val boost = if (jumpBoost >= 0) JUMP_BOOST_PER_LEVEL * (jumpBoost + 1) else 0.0

            return BASE_JUMP_POWER * below.jumpFactor() + boost
        }
    }

    private class Scan(
        val waterTop: Double = Double.NEGATIVE_INFINITY,
        val climbing: Boolean = false,
        val scaffolding: Boolean = false,
        val stuckMultiplier: Vec? = null,
        val cobweb: Boolean = false,
        val feet: Block? = null,
        val exemption: MovementExemption? = null,
    )
}
