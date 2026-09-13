package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopProjectiles
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.seconds

private const val SIZE_X = 4
private const val SIZE_Y = 3
private const val SIZE_Z = 3
private const val THROW_POWER = 1.4
private val WEB_DURATION = 10.seconds

object SeekerWebGrenadeShopItem : ShopItem {
    const val PROJECTILE_KIND = "web_grenade"

    override val id = "seeker_web_grenade"
    override val price = 7
    override val displayName = "Netzgranate"
    override val description = "Wirf ein Netz, das 10 Sekunden lang Spinnweben spannt"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/web_grenade").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!ShopProjectiles.launch(player, PROJECTILE_KIND, EntityType.EGG, THROW_POWER)) {
            return false
        }

        player.playSound(GeckoSounds.SHOP_NET_TRAP, Sound.Emitter.self())

        return true
    }

    fun detonate(instance: Instance, position: Pos) {
        val origin = position.asBlockVec()
        val placed = mutableListOf<BlockVec>()

        offsets(SIZE_X).forEach { x ->
            offsets(SIZE_Y).forEach { y ->
                offsets(SIZE_Z).forEach { z ->
                    val target = origin.add(x, y, z)

                    if (instance.getBlock(target).isAir) {
                        instance.setBlock(
                            target.blockX(),
                            target.blockY(),
                            target.blockZ(),
                            Block.COBWEB
                        )
                        placed.add(target)
                    }
                }
            }
        }

        if (placed.isEmpty()) {
            return
        }

        instance.playSound(GeckoSounds.SHOP_NET_TRAP, position.x, position.y, position.z)

        geckoScope.launch {
            delay(WEB_DURATION)

            placed.forEach {
                if (instance.getBlock(it).compare(Block.COBWEB)) {
                    instance.setBlock(it.blockX(), it.blockY(), it.blockZ(), Block.AIR)
                }
            }
        }
    }

    private fun offsets(size: Int) = -(size / 2)..(size - 1) / 2
}
