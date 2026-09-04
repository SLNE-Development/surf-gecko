package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialKeys
import java.util.*

data class GeckoGamePlayer(
    val playerUuid: UUID,
    var role: GeckoGameRole,
) {
    val player
        get() = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid)
            ?: error("Player $playerUuid not found")

    fun applyEquipment() = when (role) {
        GeckoGameRole.SEEKER -> {
            player.scheduleNextTick {
                player.inventory.clear()
                player.inventory.setEquipment(EquipmentSlot.HELMET, player.heldSlot, SEEKER_HELMET)
                player.inventory.setEquipment(
                    EquipmentSlot.HELMET,
                    player.heldSlot,
                    SEEKER_CHESTPLATE
                )
                player.inventory.setEquipment(
                    EquipmentSlot.HELMET,
                    player.heldSlot,
                    SEEKER_LEGGINGS
                )
                player.inventory.setEquipment(EquipmentSlot.HELMET, player.heldSlot, SEEKER_BOOTS)
            }
        }

        GeckoGameRole.HIDER -> {
            player.inventory.clear()
        }

        GeckoGameRole.SPECTATOR -> {
            player.inventory.clear()
        }
    }

    fun applyGameMode() = when (role) {
        GeckoGameRole.SEEKER -> {
            player.gameMode = GameMode.ADVENTURE
        }

        GeckoGameRole.HIDER -> {
            player.gameMode = GameMode.ADVENTURE
        }

        GeckoGameRole.SPECTATOR -> {
            player.gameMode = GameMode.SPECTATOR
        }
    }

    fun teleportToSpawn(map: GeckoMap) = when (role) {
        GeckoGameRole.SEEKER -> player.teleport(map.mapLocations.seekerSpawn)
        GeckoGameRole.HIDER -> player.teleport(map.mapLocations.spawn)
        GeckoGameRole.SPECTATOR -> player.teleport(map.mapLocations.spawn)
    }

    companion object {
        private val SEEKER_HELMET =
            ItemStack.of(Material.fromKey(MaterialKeys.LEATHER_HELMET.key())).builder().set(
                DataComponents.DYED_COLOR, TextColor.color(194, 58, 58)
            ).build()

        private val SEEKER_CHESTPLATE =
            ItemStack.of(Material.fromKey(MaterialKeys.LEATHER_CHESTPLATE.key())).builder().set(
                DataComponents.DYED_COLOR, TextColor.color(194, 58, 58)
            ).build()

        private val SEEKER_LEGGINGS =
            ItemStack.of(Material.fromKey(MaterialKeys.LEATHER_LEGGINGS.key())).builder().set(
                DataComponents.DYED_COLOR, TextColor.color(194, 58, 58)
            ).build()

        private val SEEKER_BOOTS =
            ItemStack.of(Material.fromKey(MaterialKeys.LEATHER_BOOTS.key())).builder().set(
                DataComponents.DYED_COLOR, TextColor.color(194, 58, 58)
            ).build()
    }
}
