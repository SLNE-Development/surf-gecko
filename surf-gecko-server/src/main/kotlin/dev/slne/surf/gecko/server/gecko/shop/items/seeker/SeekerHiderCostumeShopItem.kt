package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.COLOR_HIDER
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.costume.CostumeEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.TooltipDisplay
import kotlin.time.Duration.Companion.seconds

object SeekerHiderCostumeShopItem : ShopItem {
    override val id = "seeker_hider_costume"
    override val price = 8
    override val displayName = "Verstecker Kostüm"
    override val description = "Verkleidet dich 20 Sekunden lang als Verstecker"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)
    override val types = listOf(ShopItemType.SEEKER_ATTACK)

    override val item: ItemStack = ItemStack.of(Material.LEATHER_CHESTPLATE).builder()
        .set(DataComponents.DYED_COLOR, COLOR_HIDER)
        .set(
            DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay(false, setOf(DataComponents.DYED_COLOR, DataComponents.ATTRIBUTE_MODIFIERS))
        )
        .build()

    private val costume = GeckoGamePlayer.SEEKER_ARMOR.mapValues { ItemStack.AIR }

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!playOnce(player, 20.seconds, CostumeEffect(player, costume))) {
            player.sendText {
                appendPrefix()
                geckoPrimary("Du trägst bereits ein Verstecker Kostüm.")
            }
            return false
        }

        player.playSound(GeckoSounds.SHOP_SHIELD, Sound.Emitter.self())
        player.sendText {
            appendPrefix()
            geckoPrimary("Du siehst 20 Sekunden lang wie ein Verstecker aus.")
        }

        return true
    }
}
