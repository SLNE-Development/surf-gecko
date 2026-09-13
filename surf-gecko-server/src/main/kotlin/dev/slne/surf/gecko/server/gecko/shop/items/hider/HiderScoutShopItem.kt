package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoGlowEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopItemUsages
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

object HiderScoutShopItem : ShopItem {
    override val id = "hider_scout"
    override val price = 6
    override val displayName = "Ausguck"
    override val description = "Zeigt dir 10 Sekunden lang alle Sucher"
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/scout").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        val seekers = game.activeSeekers()

        if (seekers.isEmpty()) {
            return true
        }

        if (!ShopItemUsages.start(id, player)) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Du bist bereits auf dem Ausguck.")
            }
            return false
        }

        player.playSound(GeckoSounds.SHOP_SPY_DEVICE, Sound.Emitter.self())

        geckoScope.launch {
            try {
                GeckoGlowEffect.glow(player, seekers, 10.seconds)
            } finally {
                ShopItemUsages.finish(id, player)
            }
        }

        return true
    }
}
