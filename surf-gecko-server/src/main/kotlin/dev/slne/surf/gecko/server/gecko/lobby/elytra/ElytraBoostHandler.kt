package dev.slne.surf.gecko.server.gecko.lobby.elytra

import dev.slne.minestom.lobby.api.player.playSpinAttackAnimation
import dev.slne.surf.api.core.messages.adventure.hasPermission
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.minestom.builder.buildItem
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.ServerFlag
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Unit

object ElytraBoostHandler {
    fun checkAndBoost(player: Player) {
        if (!player.hasPermission(PermissionList.ELYTRA_BOOST)) {
            return
        }

        if (ElytraBoostTracker.isBoosting(player.uuid)) {
            boostFlight(player)
            return
        }

        if (player.isFlyingWithElytra) {
            return
        }

        ElytraBoostTracker.startBoosting(player.uuid)

        player.spawnCloudParticles(player.position)
        player.playSound(true) {
            type(SoundEvent.ENTITY_EGG_THROW)
        }

        ElytraBoostTracker.markBoosted(player.uuid)

        player.setEquipment(EquipmentSlot.CHESTPLATE, elytraItem)
        player.isFlyingWithElytra = true

        val direction = player.position.direction().normalize()
        player.velocity = direction.mul(2.0).blocksPerTick()
    }


    fun boostFlight(player: Player) {
        if (!ElytraBoostTracker.isBoosting(player.uuid)) {
            return
        }

        if (ElytraBoostTracker.isOnCooldown(player.uuid)) {
            return
        }

        player.playSpinAttackAnimation(durationTicks = 20)
        player.spawnCloudParticles(player.position)
        player.playSound(true) {
            type(SoundEvent.ITEM_TRIDENT_RIPTIDE_1)
        }

        val direction = player.position.direction().normalize()
        player.velocity = direction.mul(2.5).blocksPerTick()

        ElytraBoostTracker.markBoosted(player.uuid)
    }

    fun clearBoost(player: Player) {
        if (ElytraBoostTracker.clear(player.uuid)) {
            player.setEquipment(EquipmentSlot.CHESTPLATE, ItemStack.AIR)
        }
    }

    private fun Player.spawnCloudParticles(position: Point) {
        sendPacketToViewersAndSelf(
            ParticlePacket(Particle.CLOUD, position, Vec(0.5, 0.0, 0.5), 0.1f, 25)
        )
    }

    val elytraItem by lazy {
        buildItem(Material.ELYTRA) {
            displayName {
                geckoPrimary("Elytra Boost")
            }

            builder.set(DataComponents.UNBREAKABLE, Unit.INSTANCE)
        }
    }

    private fun Vec.blocksPerTick(): Vec = mul(ServerFlag.SERVER_TICKS_PER_SECOND.toDouble())
}