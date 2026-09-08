package dev.slne.surf.gecko.server.gecko.map.mechanic

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.util.GECKO_SECONDARY
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerInputEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockChangePacket
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object VentMechanic : GeckoMapMechanic, MinestomListener {
    override val listeners = listOf<MinestomListener>(this)

    private val ventBlock = Block.IRON_TRAPDOOR
    private const val LAUNCH_SPEED = 6.0
    private val VENT_ENTRY_DURATION = 750.milliseconds

    private lateinit var job: Job

    override suspend fun start() {
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            GeckoGameManager.getGames().forEach { game ->
                game.gamePlayers.forEach { gamePlayer ->
                    val player = gamePlayer.player

                    findVent(player) ?: return@forEach

                    player.sendActionBar(buildText {
                        geckoPrimary("Drücke ")
                        translatable("key.sneak", GECKO_SECONDARY)
                        geckoPrimary(" um den Schacht zu betreten")
                    })
                }
            }
        }
    }

    override suspend fun stop() {
        if (::job.isInitialized && job.isActive) {
            job.cancel()
        }
    }

    @EventHandler
    fun onSneak(event: PlayerInputEvent) {
        if (!event.hasPressedShiftKey()) {
            return
        }

        val player = event.player
        val vent = findVent(player) ?: return

        enterVent(player)

        val toVent = Vec(
            vent.x() + 0.5 - player.position.x,
            0.0,
            vent.z() + 0.5 - player.position.z
        )

        player.velocity = toVent.normalize().mul(LAUNCH_SPEED)
    }

    private fun findVent(player: Player): BlockVec? {
        val instance = player.instance ?: return null
        val pos = player.position
        val facing = facing(player)

        val stepX: Int
        val stepZ: Int
        if (abs(facing.x) > abs(facing.z)) {
            stepX = if (facing.x > 0) 1 else -1
            stepZ = 0
        } else {
            stepX = 0
            stepZ = if (facing.z > 0) 1 else -1
        }

        val vent = BlockVec(pos.blockX() + stepX, pos.blockY(), pos.blockZ() + stepZ)

        return if (instance.getBlock(vent).compare(ventBlock)) vent else null
    }

    private fun enterVent(player: Player) {
        val instance = player.instance ?: return
        val head = player.position.asBlockVec().add(0, 1, 0)
        val headBlock = instance.getBlock(head)

        player.entityMeta.isSwimming = true
        player.sendPacket(BlockChangePacket(head, Block.BARRIER))

        geckoScope.launch {
            delay(VENT_ENTRY_DURATION)

            if (!player.isOnline) {
                return@launch
            }

            player.entityMeta.isSwimming = false
            player.sendPacket(BlockChangePacket(head, headBlock))
        }
    }

    private fun facing(player: Player): Vec {
        val yaw = Math.toRadians(player.position.yaw.toDouble())
        return Vec(-sin(yaw), 0.0, cos(yaw))
    }
}
