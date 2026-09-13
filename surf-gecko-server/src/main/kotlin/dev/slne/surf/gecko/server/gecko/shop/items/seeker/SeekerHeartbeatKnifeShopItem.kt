package dev.slne.surf.gecko.server.gecko.shop.items.seeker

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.heartbeat.GeckoHeartbeatPulse
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.shop.ShopItem
import dev.slne.surf.gecko.server.gecko.shop.effect.ShopItemUsages
import dev.slne.surf.gecko.server.gecko.shop.nearestTo
import dev.slne.surf.gecko.server.gecko.shop.sendShopItemMessage
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.util.withTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.milliseconds

private const val DURATION_MILLIS = 15_000L
private const val RADIUS = 25.0

object SeekerHeartbeatKnifeShopItem : ShopItem {
    override val id = "seeker_heartbeat_knife"
    override val price = 6
    override val displayName = "Herzschlag-Messer"
    override val description = "Höre 15 Sekunden lang den Herzschlag des nähesten Versteckers"
    override val maps = null
    override val roles = listOf(GeckoGameRole.SEEKER)

    private val model = ItemStack.of(Material.PAPER).builder()
        .set(DataComponents.ITEM_MODEL, "gecko:shop/heartbeat_knife").build()

    override val displayItem: ItemStack = model
    override val inventoryItem: ItemStack = model.builder().withTag(ShopItem.ID_TAG, id).build()

    override fun onUse(player: Player): Boolean {
        val game = GeckoGameManager.findGame(player.uuid) ?: return false
        val gamePlayer = game.findGamePlayer(player.uuid) ?: return false

        if (!game.state.isGame() || gamePlayer.role != GeckoGameRole.SEEKER) {
            return false
        }

        if (!ShopItemUsages.start(id, player)) {
            player.sendShopItemMessage("Dein Herzschlag-Messer ist bereits aktiv.")
            return false
        }

        player.sendShopItemMessage("Das Herzschlag-Messer ist für 15 Sekunden scharf.")

        geckoScope.launch {
            try {
                val until = System.currentTimeMillis() + DURATION_MILLIS
                var nextBeatAt = 0L

                while (System.currentTimeMillis() < until && player.isOnline) {
                    val now = System.currentTimeMillis()
                    val distance = nearestHiderDistance(player)

                    if (distance != null && distance <= RADIUS && now >= nextBeatAt) {
                        val proximity = GeckoHeartbeatPulse.proximityFor(distance, RADIUS)

                        player.playSound(
                            GeckoSounds.heartbeat(
                                GeckoHeartbeatPulse.volumeFor(proximity),
                                GeckoHeartbeatPulse.pitchFor(proximity)
                            ),
                            Sound.Emitter.self()
                        )

                        nextBeatAt = now + GeckoHeartbeatPulse.intervalFor(proximity)
                    }

                    delay(GeckoHeartbeatPulse.TICK_MILLIS.milliseconds)
                }
            } finally {
                ShopItemUsages.finish(id, player)
            }
        }

        return true
    }

    private fun nearestHiderDistance(player: Player): Double? {
        val game = GeckoGameManager.findGame(player.uuid) ?: return null
        val hiders = game.gamePlayers
            .filter { it.role == GeckoGameRole.HIDER && !it.awaitingRespawn }
            .mapNotNull { it.playerOrNull }

        return hiders.nearestTo(player)?.position?.distance(player.position)
    }
}
