package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.activeHiders
import dev.slne.surf.gecko.server.gecko.shop.activeSeekers
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.BeamEffect
import dev.slne.surf.gecko.server.gecko.shop.effect.beam.ParticleBeam
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
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
    private val beam = ParticleBeam(Colors.ERROR, height = 50.0)

    override val displayItem: ItemStack = itemBase
    override val inventoryItem: ItemStack = itemBase.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        val origins = game.activeHiders().map { it.position.asVec() }

        if (origins.isEmpty()) {
            return true
        }

        val seekers = game.activeSeekers().toSet()

        seekers.forEach { it.playSound(GeckoSounds.SHOP_LASER, Sound.Emitter.self()) }

        geckoScope.launch {
            BeamEffect(seekers, origins, beam).playFor(7.5.seconds)
        }

        return true
    }
}
