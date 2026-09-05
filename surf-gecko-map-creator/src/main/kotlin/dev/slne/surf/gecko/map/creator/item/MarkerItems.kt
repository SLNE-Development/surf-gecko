package dev.slne.surf.gecko.map.creator.item

import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.gecko.map.creator.draft.GeckoPoiType
import dev.slne.surf.gecko.map.creator.plugin
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object MarkerItems {
    const val TOOLKIT_SLOT = 4

    private val markerKey = NamespacedKey(plugin, "marker-type")
    private val toolkitKey = NamespacedKey(plugin, "toolkit")

    fun giveTools(player: Player) {
        val inventory = player.inventory

        for (type in GeckoPoiType.entries) {
            inventory.setItem(type.hotbarSlot, markerItem(type))
        }

        inventory.setItem(TOOLKIT_SLOT, toolkitItem())
    }

    fun clearTools(player: Player) {
        val inventory = player.inventory

        for (index in 0 until inventory.size) {
            val item = inventory.getItem(index) ?: continue
            if (markerType(item) != null || isToolkit(item)) {
                inventory.setItem(index, null)
            }
        }
    }

    fun markerType(item: ItemStack?): GeckoPoiType? {
        val id = item?.itemMeta?.persistentDataContainer?.get(markerKey, PersistentDataType.STRING)
        return id?.let(GeckoPoiType::byId)
    }

    fun isToolkit(item: ItemStack?) =
        item?.itemMeta?.persistentDataContainer?.has(toolkitKey, PersistentDataType.BYTE) == true

    private fun markerItem(type: GeckoPoiType) = buildItem(type.itemMaterial) {
        displayName {
            text(type.displayName, type.color, TextDecoration.BOLD)
        }

        buildLore {
            line { info("Rechtsklick auf Block "); variableValue("-> Marker auf die Blockmitte") }
            line { info("Schleichen + Rechtsklick "); variableValue("-> exakte Spielerposition") }
            line { info("Linksklick "); variableValue("-> Marker entfernen") }
            emptyLine()
            if (type.multiple) {
                line { spacer("Mehrfach-POI - beliebig viele Marker") }
            } else {
                line { spacer("Einzel-POI - ein neuer Marker ersetzt den alten") }
            }
            if (type.withRotation) {
                line { spacer("Blickrichtung wird als Yaw/Pitch gespeichert") }
            }
        }

        meta {
            persistentDataContainer.set(markerKey, PersistentDataType.STRING, type.id)
            isUnbreakable = true
        }
    }

    private fun toolkitItem() = buildItem(Material.NETHER_STAR) {
        displayName {
            primary("Gecko Map Toolkit", TextDecoration.BOLD)
        }

        buildLore {
            line { info("Rechtsklick "); variableValue("-> Übersicht") }
            line { info("Schleichen + Rechtsklick "); variableValue("-> Export") }
        }

        meta {
            persistentDataContainer.set(toolkitKey, PersistentDataType.BYTE, 1.toByte())
            isUnbreakable = true
        }
    }
}
