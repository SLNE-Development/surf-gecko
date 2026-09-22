package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.effect.glow.GlowEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.shop.nearestTo
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

object SeekerSpyDeviceShopItem : ShopItem {
    override val id = "seeker_spy_device"
    override val price = 8
    override val displayName = "Spionagegerät"
    override val description = "Markiert den nähesten Verstecker 30 Sekunden lang nur für dich"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    override val item: ItemStack = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/spy_device").build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        val target = game.activeHiders().nearestTo(player) ?: return true

        if (!playOnce(player, 30.seconds, GlowEffect(player, listOf(target)))) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Dein Spionagegerät ist bereits aktiv.")
            }
            return false
        }

        player.playSound(GeckoSounds.SHOP_SPY_DEVICE, Sound.Emitter.self())

        return true
    }
}
