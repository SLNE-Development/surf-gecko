package dev.slne.surf.gecko.server.gecko.lobby.npc

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.NpcKind
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.types.AbstractNpcEntity
import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.inventory.EquipmentHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.EntityAttributesPacket
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket
import net.minestom.server.network.player.ResolvableProfile
import java.util.*

class EquipableMannequinNpc(
    private val name: String,
    hologramText: Component,
    profile: ResolvableProfile?,
    private val scale: Double,
    hologramOffset: Vec,
    description: Component,
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(EntityType.MANNEQUIN, uuid),
    EquipmentHandler {
    private val equipment = mutableMapOf<EquipmentSlot, ItemStack>()

    init {
        editEntityMeta<MannequinMeta> { meta ->
            profile?.let { meta.profile = it }
            meta.isImmovable = true
            meta.description = description
            meta.displayedSkinParts = ALL_SKIN_PARTS
        }
        setNoGravity(true)
        nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM
        textDisplayController = TextDisplayController(hologramText, hologramOffset)
    }

    override val kind: NpcKind get() = NpcKind.MANNEQUIN
    override val displayName: String get() = name

    override fun spawn() {
        StomNPCs.manager().register(this)

        val instance = instance ?: return
        scheduler().scheduleNextTick { textDisplayController?.attachTo(this, instance) }
    }


    @Suppress("UnstableApiUsage")
    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)

        if (scale != 1.0) {
            player.sendPacket(
                EntityAttributesPacket(
                    entityId,
                    listOf(EntityAttributesPacket.Property(Attribute.SCALE, scale, emptyList()))
                )
            )
        }

        if (equipment.isNotEmpty()) {
            updateEquipment(player)
        }
    }

    fun updateDisplayName(hologramText: Component) {
        textDisplayController?.updateText(hologramText)
    }

    override fun getEquipment(slot: EquipmentSlot?) = equipment[slot]

    override fun setEquipment(
        slot: EquipmentSlot?,
        itemStack: ItemStack?
    ) {
        if (slot == null) {
            return
        }

        if (itemStack == null) {
            equipment.remove(slot)
        } else {
            equipment[slot] = itemStack
        }

        entity.instance.players.forEach {
            updateEquipment(it)
        }
    }

    fun setEquipmentMap(equipmentMap: Map<EquipmentSlot, ItemStack>) {
        equipment.clear()
        equipment.putAll(equipmentMap)
    }

    private fun updateEquipment(player: Player) {
        player.sendPacket(
            EntityEquipmentPacket(
                entityId,
                equipment
            )
        )
    }

    companion object {
        private const val ALL_SKIN_PARTS: Byte = 0x7F
    }
}