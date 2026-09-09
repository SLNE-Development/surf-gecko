package dev.slne.surf.gecko.server.anticheat

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.surf.gecko.server.anticheat.report.AntiCheatDisplay
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTeleportEvent
import net.minestom.server.event.entity.EntityVelocityEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.event.player.PlayerRespawnEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerTickEndEvent
import net.minestom.server.network.packet.client.play.ClientPlayerPositionStatusPacket

@Singleton
class AntiCheatService @Inject constructor(
    private val config: Config,
    private val luckPerms: LuckPermsService,
) : GeckoService, EventRegistrar {

    private val settings get() = config.antiCheat

    override fun register(node: EventNode<Event>) {
        if (!settings.enabled) {
            return
        }

        node.addListener(PlayerSpawnEvent::class.java) { event ->
            track(event.player)
        }

        node.addListener(PlayerDisconnectEvent::class.java) { event ->
            AntiCheatTracker.remove(event.player)
            AntiCheatDisplay.forget(event.player)
        }

        node.addListener(PlayerMoveEvent::class.java) { event ->
            tracked(event.player)?.onMove(event.newPosition, event.isOnGround)
        }

        node.addListener(PlayerPacketEvent::class.java) { event ->
            val packet = event.packet

            if (packet is ClientPlayerPositionStatusPacket) {
                tracked(event.player)?.onGroundStatus(packet.onGround())
            }
        }

        node.addListener(PlayerTickEndEvent::class.java) { event ->
            tracked(event.player)?.tick()
        }

        node.addListener(PlayerRespawnEvent::class.java) { event ->
            tracked(event.player)?.onRespawn()
        }

        node.addListener(EntityTeleportEvent::class.java) { event ->
            (event.entity as? Player)?.let { tracked(it)?.onTeleport() }
        }

        node.addListener(EntityVelocityEvent::class.java) { event ->
            (event.entity as? Player)?.let { tracked(it)?.onVelocity() }
        }
    }

    override suspend fun start() {
        if (!settings.enabled) {
            return
        }

        AntiCheatDisplay.init()
    }

    override suspend fun stop() {
        AntiCheatDisplay.shutdown()
        AntiCheatTracker.clear()
    }

    private fun track(player: Player) {
        if (luckPerms.hasPermission(player.uuid, PermissionList.ANTICHEAT_BYPASS).asBoolean()) {
            return
        }

        AntiCheatTracker.register(player, settings, AntiCheatDisplay::report)
    }

    private fun tracked(player: Player) = AntiCheatTracker.find(player)
}
