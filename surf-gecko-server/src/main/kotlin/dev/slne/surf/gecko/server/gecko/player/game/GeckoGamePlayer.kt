package dev.slne.surf.gecko.server.gecko.player.game

import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.map.GeckoMap
import dev.slne.surf.gecko.server.gecko.map.mechanic.impl.VentMechanic
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.ShopItemListener
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.gecko.util.*
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.language
import dev.slne.surf.gecko.server.i18n.render
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.i18n.translate
import dev.slne.surf.gecko.server.util.withTag
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.tag.Tag
import java.util.*
import java.util.concurrent.CompletableFuture

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
                val language = player.language
                player.inventory.clear()
                player.setCanPickupItem(true)
                seekerArmor(language).forEach { (slot, item) ->
                    player.inventory.setEquipment(slot, player.heldSlot, item)
                }
                player.inventory.setItemStack(0, seekerSword(language))
                player.inventory.setItemStack(1, seekerBow(language))
                player.inventory.setItemStack(
                    17,
                    ItemStack.of(Material.ARROW).withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)
                )

                ShopItemListener.giveShop(this)
            }
        }

        GeckoGameRole.HIDER -> {
            player.inventory.clear()
            player.setCanPickupItem(true)
            ShopItemListener.giveShop(this)
        }

        GeckoGameRole.SPECTATOR -> {
            player.inventory.clear()
            player.setCanPickupItem(false)
        }
    }

    fun sendRoleMessage() {
        player.sendTranslated("game.role.assigned", "role" to role.displayText)
        player.showTitle {
            title = player.render(role.displayText)
            subtitle = player.translate("game.role.subtitle", "description" to role.description)
        }
    }

    fun applySpeed() {
        val player = playerOrNull ?: return
        val settings = GeckoGameManager.findGame(playerUuid)?.settings ?: return

        val speedFactor = if (VentMechanic.isInVent(playerUuid)) {
            settings.ventSpeedFactor
        } else when (role) {
            GeckoGameRole.SEEKER -> settings.seekerSpeedFactor
            GeckoGameRole.HIDER -> settings.hiderSpeedFactor
            GeckoGameRole.SPECTATOR -> 1.0
        }

        player.applyMovementSpeedFactor(speedFactor)
    }

    fun resetSpeed() {
        playerOrNull?.resetGeckoSpeed()
    }

    fun updateSocialGroup() {
        SocialGroupManager.update(this)
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

    fun endSpectating(map: GeckoMap) {
        val player = playerOrNull ?: return

        player.gameMode = GameMode.ADVENTURE
        player.heal()
        player.teleport(map.mapLocations.spawn)
    }

    fun respawnAsSeeker(map: GeckoMap) {
        player.isInvulnerable = false
        player.heal()
        player.teleport(map.mapLocations.spawn)
    }

    fun teleportToSpawn(map: GeckoMap): CompletableFuture<Void> = when (role) {
        GeckoGameRole.SEEKER -> player.teleport(map.mapLocations.seekerSpawn)
        GeckoGameRole.HIDER -> player.teleport(map.mapLocations.spawn)
        GeckoGameRole.SPECTATOR -> player.teleport(map.mapLocations.spawn)
    }

    companion object {
        private val SEEKER_COLOR = TextColor.color(227, 36, 36)
        val SEEKER_GEAR_TAG: Tag<String> = Tag.String("gecko_seeker_gear")

        private fun seekerGear(language: GeckoLanguage, material: Material, id: String) =
            ItemStack.of(material).builder()
                .set(
                    DataComponents.TOOLTIP_DISPLAY,
                    TooltipDisplay(false, setOf(DataComponents.ENCHANTMENTS))
                )
                .set(
                    DataComponents.ITEM_NAME,
                    GeckoTranslations.render(language, "game.gear.seeker.$id").colorIfAbsent(SEEKER_COLOR)
                )
                .withTag(SEEKER_GEAR_TAG, id)
                .withTag(GeckoPlayerListener.GECKO_ITEM_TAG, true)

        private fun seekerArmorPiece(language: GeckoLanguage, material: Material, id: String) =
            seekerGear(language, material, id)
                .set(DataComponents.DYED_COLOR, SEEKER_COLOR)
                .build()

        fun seekerArmor(language: GeckoLanguage) = mapOf(
            EquipmentSlot.HELMET to seekerArmorPiece(language, Material.LEATHER_HELMET, "helmet"),
            EquipmentSlot.CHESTPLATE to seekerArmorPiece(language, Material.LEATHER_CHESTPLATE, "chestplate"),
            EquipmentSlot.LEGGINGS to seekerArmorPiece(language, Material.LEATHER_LEGGINGS, "leggings"),
            EquipmentSlot.BOOTS to seekerArmorPiece(language, Material.LEATHER_BOOTS, "boots")
        )

        fun seekerSword(language: GeckoLanguage) = seekerGear(language, Material.WOODEN_SWORD, "sword").build()

        fun seekerBow(language: GeckoLanguage) = seekerGear(language, Material.BOW, "bow")
            .set(DataComponents.ENCHANTMENTS, EnchantmentList(mapOf(Enchantment.INFINITY to 1)))
            .build()

        fun seekerGear(language: GeckoLanguage, id: String): ItemStack? = when (id) {
            "sword" -> seekerSword(language)
            "bow" -> seekerBow(language)
            else -> seekerArmor(language).values.firstOrNull { it.getTag(SEEKER_GEAR_TAG) == id }
        }

        fun relocalizeSeekerGear(player: Player) {
            val inventory = player.inventory

            for (slot in 0 until inventory.size) {
                val item = inventory.getItemStack(slot)
                val id = item.getTag(SEEKER_GEAR_TAG) ?: continue
                val localized = seekerGear(player.language, id) ?: continue

                inventory.setItemStack(slot, localized.withAmount(item.amount()))
            }
        }
    }
}
