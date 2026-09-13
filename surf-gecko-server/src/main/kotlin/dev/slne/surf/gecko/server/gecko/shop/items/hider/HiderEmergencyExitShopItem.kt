package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.secureRandom
import dev.slne.surf.gecko.server.util.withTag
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

private const val CANDIDATE_COUNT = 10

object HiderEmergencyExitShopItem : ShopItem {
    override val id = "hider_emergency_exit"
    override val price = 9
    override val displayName = "Notausgang"
    override val description = "Bringt dich an einen Ort weit weg von allen Suchern"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/emergency_exit").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        if ((game.gameTimerSeconds ?: 0) < game.settings.roundTimeSeconds * 0.1) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Der Notausgang kann nicht mehr benutzt werden.")
            }
            return false
        }

        val seekers = game.activeSeekers()
        val candidates = game.settings.map.mapLocations.orbSpawns
            .sortedByDescending { spawn ->
                seekers.minOfOrNull { it.position.distance(spawn) } ?: Double.MAX_VALUE
            }
            .take(CANDIDATE_COUNT)

        if (candidates.isEmpty()) {
            player.sendShopItemMessage("Der Notausgang führt auf dieser Map nirgendwo hin.")
            return false
        }

        player.playSound(GeckoSounds.SHOP_TELEPORT, Sound.Emitter.self())
        player.teleport(candidates.secureRandom())
        player.sendShopItemMessage("Der Notausgang hat dich in Sicherheit gebracht.")

        return true
    }
}
