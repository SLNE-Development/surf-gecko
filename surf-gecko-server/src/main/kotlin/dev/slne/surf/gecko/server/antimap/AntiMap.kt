package dev.slne.surf.gecko.server.antimap

import dev.slne.surf.gecko.server.event.EventHandler
import dev.slne.surf.gecko.server.event.MinestomListener
import dev.slne.surf.gecko.server.event.register
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerPluginMessageEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.network.packet.server.common.PluginMessagePacket
import net.minestom.server.timer.TaskSchedule

object AntiMap : MinestomListener {
    fun init() = register()

    @EventHandler
    fun onSpawn(event: PlayerSpawnEvent) {
        if (!event.isFirstSpawn) {
            return
        }

        val player = event.player

        player.scheduler()
            .buildTask {
                player.send(XAERO_MINIMAP_CHANNEL, xaeroHandshake)
                player.send(XAERO_WORLDMAP_CHANNEL, xaeroHandshake)
                player.send(XAERO_MINIMAP_CHANNEL, xaeroSettings)
                player.send(XAERO_WORLDMAP_CHANNEL, xaeroSettings)
                player.send(VOXELMAP_SETTINGS_CHANNEL, voxelSettings)
                player.send(JOURNEYMAP_PERMISSION_CHANNEL, journeymapPermissions)
            }
            .delay(TaskSchedule.tick(40))
            .schedule()
    }

    @EventHandler
    fun onPluginMessage(event: PlayerPluginMessageEvent) {
        val channel = event.identifier
        val player = event.player

        when (channel) {
            XAERO_MINIMAP_CHANNEL, XAERO_WORLDMAP_CHANNEL -> {
                if (event.message.firstOrNull()?.toInt() == 1) {
                    player.send(channel, xaeroSettings)
                }
            }

            JOURNEYMAP_VERSION_CHANNEL -> player.send(channel, journeymapVersion)
            JOURNEYMAP_PERMISSION_CHANNEL -> player.send(channel, journeymapPermissions)
        }
    }
}

private fun Player.send(channel: String, payload: ByteArray) =
    sendPacket(PluginMessagePacket(channel, payload))
