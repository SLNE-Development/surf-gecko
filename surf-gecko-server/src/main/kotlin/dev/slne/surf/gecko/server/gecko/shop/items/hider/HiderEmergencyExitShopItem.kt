package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.secureRandom
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object HiderEmergencyExitShopItem : ShopItem {
    override val id = "hider_emergency_exit"
    override val price = 9
    override val displayName = "Notausgang"
    override val description = "Entkomme den Suchern schnell"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)
    override val item: ItemStack = ItemStack.of(Material.IRON_DOOR)

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
            .take(10)

        if (candidates.isEmpty()) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Der Notausgang konnte nicht benutzt werden, da es keinen sicheren Ort gibt.")
            }
            return false
        }

        player.playSound(GeckoSounds.SHOP_TELEPORT, Sound.Emitter.self())
        player.teleport(candidates.secureRandom())
        player.sendText {
            appendPrefix()
            geckoPrimary("Der Notausgang hat dich in Sicherheit gebracht.")
        }

        return true
    }
}
