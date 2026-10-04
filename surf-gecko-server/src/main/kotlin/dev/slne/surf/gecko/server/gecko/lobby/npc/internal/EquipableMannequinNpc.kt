package dev.slne.surf.gecko.server.gecko.lobby.npc

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.NpcKind
import codes.bed.minestom.npc.types.AbstractNpcEntity
import dev.slne.surf.api.minestom.extension.editEntityMeta
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.LocalizedComponent
import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import dev.slne.surf.gecko.server.i18n.language
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.MetadataDef
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.inventory.EquipmentHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.EntityAttributesPacket
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.player.ResolvableProfile
import java.util.*

class EquipableMannequinNpc(
    private val name: String,
    hologramText: LocalizedComponent,
    profile: ResolvableProfile?,
    private val scale: Double,
    private val hologramOffset: Vec,
    description: Component,
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(EntityType.MANNEQUIN, uuid),
    EquipmentHandler {
    private val equipment = mutableMapOf<EquipmentSlot, ItemStack>()
    private val hologram = LocalizedTextDisplay(hologramText)

    init {
        editEntityMeta<MannequinMeta> { meta ->
            profile?.let { meta.profile = it }
            meta.isImmovable = true
            meta.description = description
            meta.displayedSkinParts = ALL_SKIN_PARTS
        }
        setNoGravity(true)
        nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM
        PlayerLanguages.onChange { hologram.refresh(it) }
    }

    override val kind: NpcKind get() = NpcKind.MANNEQUIN
    override val displayName: String get() = name

    override fun spawn() {
        StomNPCs.manager().register(this)

        val instance = instance ?: return
        scheduler().scheduleNextTick {
            hologram.setInstance(instance, position.add(hologramOffset))
            StomNPCs.manager().registerEntity(hologram.uuid, this)
        }
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

    fun updateDisplayName(hologramText: LocalizedComponent) {
        hologram.updateText(hologramText)
    }

    override fun remove() {
        StomNPCs.manager().unregisterEntity(hologram.uuid)
        hologram.remove()
        super.remove()
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

private class LocalizedTextDisplay(private var text: LocalizedComponent) : Entity(EntityType.TEXT_DISPLAY) {
    init {
        setNoGravity(true)
        editEntityMeta<TextDisplayMeta> { meta ->
            meta.text = text.render(GeckoLanguage.FALLBACK)
            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            meta.isShadow = true
            meta.backgroundColor = 0x000000
        }
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        sendText(player)
    }

    fun updateText(text: LocalizedComponent) {
        this.text = text
        editEntityMeta<TextDisplayMeta> { meta -> meta.text = text.render(GeckoLanguage.FALLBACK) }
        viewers.forEach { sendText(it) }
    }

    fun refresh(player: Player) {
        if (isViewer(player)) {
            sendText(player)
        }
    }

    private fun sendText(player: Player) {
        player.sendPacket(
            EntityMetaDataPacket(
                entityId,
                mapOf(MetadataDef.TextDisplay.TEXT.index() to Metadata.Component(text.render(player.language)))
            )
        )
    }
}
