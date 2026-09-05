package dev.slne.surf.gecko.map.creator.draft

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

enum class GeckoPoiType(
    val id: String,
    val displayName: String,
    val itemMaterial: Material,
    val markerMaterial: Material,
    val color: TextColor,
    val hotbarSlot: Int,
    val multiple: Boolean,
    val withRotation: Boolean,
) {
    LOBBY_SPAWN(
        id = "lobby-spawn",
        displayName = "Lobby Spawn",
        itemMaterial = Material.LIME_CONCRETE,
        markerMaterial = Material.LIME_STAINED_GLASS,
        color = NamedTextColor.GREEN,
        hotbarSlot = 0,
        multiple = false,
        withRotation = true,
    ),
    SEEKER_SPAWN(
        id = "seeker-spawn",
        displayName = "Seeker Spawn",
        itemMaterial = Material.RED_CONCRETE,
        markerMaterial = Material.RED_STAINED_GLASS,
        color = NamedTextColor.RED,
        hotbarSlot = 1,
        multiple = false,
        withRotation = true,
    ),
    SPAWN(
        id = "spawn",
        displayName = "Spawn",
        itemMaterial = Material.LIGHT_BLUE_CONCRETE,
        markerMaterial = Material.LIGHT_BLUE_STAINED_GLASS,
        color = NamedTextColor.AQUA,
        hotbarSlot = 2,
        multiple = false,
        withRotation = true,
    ),
    ORB_SPAWN(
        id = "orb-spawn",
        displayName = "Orb Spawn",
        itemMaterial = Material.GOLD_BLOCK,
        markerMaterial = Material.YELLOW_STAINED_GLASS,
        color = NamedTextColor.YELLOW,
        hotbarSlot = 3,
        multiple = true,
        withRotation = false,
    );

    val markerBlockData by lazy { markerMaterial.createBlockData() }

    companion object {
        private val byId = entries.associateBy { it.id }

        fun byId(id: String) = byId[id]
    }
}
