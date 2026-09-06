package dev.slne.surf.gecko.server.gecko.water

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGame
import kotlinx.coroutines.Job
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import kotlin.time.Duration.Companion.seconds

class GeckoWaterDamager(val game: GeckoGame) {
    private lateinit var damagerJob: Job

    fun start() {
        damagerJob = geckoAsyncScope.runAtFixedRate(1.seconds) {
            game.forEachPlayer { player ->
                if (player.instance?.getBlock(player.position)?.liquid() == true) {
                    player.damage(Damage(DamageType.DROWN, null, null, null, 2f))
                }
            }
        }
    }

    fun stop() {
        if (::damagerJob.isInitialized && damagerJob.isActive) {
            damagerJob.cancel()
        }
    }
}