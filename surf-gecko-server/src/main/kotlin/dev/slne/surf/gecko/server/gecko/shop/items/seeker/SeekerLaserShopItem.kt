package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.BlockChangePacket
import kotlin.time.Duration.Companion.seconds

object SeekerLaserShopItem : ShopItem {
    override val id = "seeker_laser"
    override val price = 12
    override val displayName = "Laser"
    override val description =
        "Markiert allen Suchern 7,5 Sekunden lang den Standort jedes Versteckers"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val itemBase = ItemStack.of(Material.BEACON)

    override val displayItem: ItemStack = itemBase
    override val inventoryItem: ItemStack = itemBase.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        val beams = game.activeHiders().map { it.position.asBlockVec().sub(0, 1, 0) }.toSet()

        if (beams.isEmpty()) {
            return true
        }

        game.activeSeekers().forEach { seeker ->
            beams.forEach { seeker.sendPacket(BlockChangePacket(it, Block.BEACON)) }
            seeker.playSound(GeckoSounds.SHOP_LASER, Sound.Emitter.self())
        }

        geckoScope.launch {
            delay(7.5.seconds)

            game.activeSeekers().forEach { seeker ->
                beams.forEach {
                    seeker.sendPacket(BlockChangePacket(it, game.instance.getBlock(it)))
                }
            }
        }

        return true
    }
}
