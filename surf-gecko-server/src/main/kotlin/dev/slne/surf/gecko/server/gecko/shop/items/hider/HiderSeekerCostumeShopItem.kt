package dev.slne.surf.gecko.server.gecko.shop.items.hider

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.COLOR_SEEKER
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.costume.CostumeEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.playOnce
import dev.slne.surf.gecko.server.gecko.shop.type.ShopItemType
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.language
import dev.slne.surf.gecko.server.i18n.sendTranslated
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.TooltipDisplay
import kotlin.time.Duration.Companion.seconds

object HiderSeekerCostumeShopItem : ShopItem {
    override val id = "hider_seeker_costume"
    override val price = 8
    override val maps = null
    override val roles = listOf(GeckoGameRole.HIDER)
    override val types = listOf(ShopItemType.HIDER_ATTACK)

    override val item: ItemStack = ItemStack.of(Material.LEATHER_CHESTPLATE).builder()
        .set(DataComponents.DYED_COLOR, COLOR_SEEKER)
        .set(
            DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay(false, setOf(DataComponents.DYED_COLOR, DataComponents.ATTRIBUTE_MODIFIERS))
        )
        .build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.HIDER) {
            return false
        }

        if (!playOnce(player, 20.seconds, CostumeEffect(player, GeckoGamePlayer.seekerArmor(player.language)))) {
            player.sendTranslated("shop.item.hider_seeker_costume.already-active")
            return false
        }

        player.playSound(GeckoSounds.SHOP_SHIELD, Sound.Emitter.self())
        player.sendTranslated("shop.item.hider_seeker_costume.activated")

        return true
    }
}
