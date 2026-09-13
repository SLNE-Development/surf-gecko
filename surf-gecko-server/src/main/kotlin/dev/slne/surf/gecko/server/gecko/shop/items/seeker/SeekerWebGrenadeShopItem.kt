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
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import kotlin.time.Duration.Companion.seconds

object SeekerWebGrenadeShopItem : ShopItem {
    override val id = "seeker_web_grenade"
    override val price = 7
    override val displayName = "Netzgranate"
    override val description = "Fange die Sucher mit einem Netz"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val itemBase = ItemStack.of(Material.COBWEB)

    override val displayItem: ItemStack = itemBase
    override val inventoryItem: ItemStack = itemBase.builder().withTag(ShopItem.ID_TAG, id).build()

    private val projectileItem = ItemStack.of(Material.COBWEB)

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!ShopProjectiles.launch(player, projectileItem, 20.0, ::detonate)) {
            return false
        }

        player.playSound(GeckoSounds.SHOP_NET_TRAP, Sound.Emitter.self())
        return true
    }

    private fun detonate(instance: Instance, position: Pos, thrower: Player) {
        val anchor = anchorFor(instance, position.asBlockVec())
        val placed = mutableListOf<BlockVec>()

        offsets(4).forEach { x ->
            offsets(3).forEach { z ->
                (0 until 3).forEach { y ->
                    val target = anchor.add(x, y, z)

                    if (instance.getBlock(target).replaceable()) {
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

        instance.playSound(GeckoSounds.SHOP_NET_TRAP, position.x, position.y, position.z)
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.CLOUD,
                position.add(0.0, 0.3, 0.0),
                Vec(0.8, 0.5, 0.8),
                0.02f,
                30
            )
        )

        geckoScope.launch {
            delay(10.seconds)

            placed.forEach {
                if (instance.getBlock(it).compare(Block.COBWEB)) {
                    instance.setBlock(it.blockX(), it.blockY(), it.blockZ(), Block.AIR)
                }
            }
        }
    }

    private fun anchorFor(instance: Instance, impact: BlockVec): BlockVec {
        var current = impact

        repeat(3) {
            if (!instance.getBlock(current).solid()) {
                return current
            }

            current = current.add(0, 1, 0)
        }

        return impact
    }

    private fun offsets(size: Int) = -(size / 2)..(size - 1) / 2
}
