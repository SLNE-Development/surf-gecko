package dev.slne.surf.gecko.server.gecko.shop.effect.costume

import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoEffect
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import kotlin.time.Duration.Companion.seconds

class CostumeEffect(
    private val player: Player,
    private val costume: Map<EquipmentSlot, ItemStack>
) : GeckoEffect(1.seconds) {
    private val original = mutableMapOf<EquipmentSlot, ItemStack>()

    override fun isActive() = player.isOnline

    override fun start() {
        costume.keys.forEach { original[it] = player.getEquipment(it) }
        wear(costume)
    }

    override fun pulse() = wear(costume)

    override fun stop() = wear(original)

    private fun wear(equipment: Map<EquipmentSlot, ItemStack>) {
        if (!player.isOnline) {
            return
        }

        equipment.forEach { (slot, item) -> player.setEquipment(slot, item) }
    }
}
