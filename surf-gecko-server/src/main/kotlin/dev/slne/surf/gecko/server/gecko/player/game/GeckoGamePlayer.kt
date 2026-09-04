package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.item.enchant.Enchantment
import java.util.*

data class GeckoGamePlayer(
    val playerUuid: UUID,
    var role: GeckoGameRole,
) {
    var respawnSecondsLeft: Int? = null

    val playerOrNull
        get() = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid)

    val player
        get() = playerOrNull ?: error("Player $playerUuid not found")

    val awaitingRespawn get() = respawnSecondsLeft != null

    fun applyEquipment() = when (role) {
        GeckoGameRole.SEEKER -> {
            player.scheduleNextTick {
                player.inventory.clear()
                player.inventory.setEquipment(EquipmentSlot.HELMET, player.heldSlot, SEEKER_HELMET)
                player.inventory.setEquipment(
                    EquipmentSlot.CHESTPLATE,
                    player.heldSlot,
                    SEEKER_CHESTPLATE
                )
                player.inventory.setEquipment(
                    EquipmentSlot.LEGGINGS,
                    player.heldSlot,
                    SEEKER_LEGGINGS
                )
                player.inventory.setEquipment(EquipmentSlot.BOOTS, player.heldSlot, SEEKER_BOOTS)
                player.inventory.setItemStack(0, SEEKER_SWORD)
                player.inventory.setItemStack(1, SEEKER_BOW)
                player.inventory.setItemStack(17, ItemStack.of(Material.ARROW))
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

    fun moveToSeekerLobby(map: GeckoMap) {
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.inventory.clear()
        player.heal()
        player.teleport(map.mapLocations.seekerSpawn)
    }

    fun clearRespawnState() {
        respawnSecondsLeft = null
        playerOrNull?.isInvulnerable = false
    }

    fun respawnAsSeeker(map: GeckoMap) {
        applyGameMode()
        applyEquipment()
        player.isInvulnerable = false
        player.heal()
        player.teleport(map.mapLocations.spawn)
    }

    fun teleportToSpawn(map: GeckoMap) = when (role) {
        GeckoGameRole.SEEKER -> player.teleport(map.mapLocations.seekerSpawn)
        GeckoGameRole.HIDER -> player.teleport(map.mapLocations.spawn)
        GeckoGameRole.SPECTATOR -> player.teleport(map.mapLocations.spawn)
    }

    companion object {
        private val SEEKER_COLOR = TextColor.color(194, 58, 58)
        private val SEEKER_HELMET =
            ItemStack.of(Material.LEATHER_HELMET).builder()
                .set(DataComponents.DYED_COLOR, SEEKER_COLOR)
                .set(
                    DataComponents.TOOLTIP_DISPLAY,
                    TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
                )
                .build()

        private val SEEKER_CHESTPLATE =
            ItemStack.of(Material.LEATHER_CHESTPLATE).builder()
                .set(DataComponents.DYED_COLOR, SEEKER_COLOR)
                .set(
                    DataComponents.TOOLTIP_DISPLAY,
                    TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
                )
                .build()

        private val SEEKER_LEGGINGS =
            ItemStack.of(Material.LEATHER_LEGGINGS).builder()
                .set(DataComponents.DYED_COLOR, SEEKER_COLOR)
                .set(
                    DataComponents.TOOLTIP_DISPLAY,
                    TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
                )
                .build()

        private val SEEKER_BOOTS =
            ItemStack.of(Material.LEATHER_BOOTS).builder()
                .set(DataComponents.DYED_COLOR, SEEKER_COLOR)
                .set(
                    DataComponents.TOOLTIP_DISPLAY,
                    TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
                )
                .build()

        private val SEEKER_SWORD = ItemStack.of(Material.WOODEN_SWORD).builder()
            .set(
                DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
            )
            .build()
        private val SEEKER_BOW = ItemStack.of(Material.BOW).builder()
            .set(DataComponents.ENCHANTMENTS, EnchantmentList(mapOf(Enchantment.INFINITY to 1)))
            .set(
                DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay(true, setOf(DataComponents.ENCHANTMENTS))
            )
            .build()
    }
}
