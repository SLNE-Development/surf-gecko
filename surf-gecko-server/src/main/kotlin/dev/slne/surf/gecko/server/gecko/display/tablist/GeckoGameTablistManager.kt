package dev.slne.surf.gecko.server.gecko.display.tablist

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.i18n.LocalizedComponent
import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import dev.slne.surf.gecko.server.i18n.translatable
import dev.slne.surf.gecko.server.i18n.translate
import kotlinx.coroutines.Job
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import kotlin.time.Duration.Companion.seconds

object GeckoGameTablistManager {
    private lateinit var job: Job

    fun init() {
        job = minestomAsyncScope.runAtFixedRate(1.seconds) {
            sendAdditions()
        }

        PlayerLanguages.onChange(::sendAdditions)
    }

    fun shutdown() {
        if (::job.isInitialized && job.isActive) {
            job.cancel()
        }
    }

    fun sendAdditions() {
        MinecraftServer.getConnectionManager().onlinePlayers.forEach(::sendAdditions)
    }

    private fun sendAdditions(player: Player) {
        player.sendPlayerListHeaderAndFooter(
            player.translate("display.tablist.header", "state" to currentState(player)),
            player.translate("display.tablist.footer")
        )
    }

    private fun currentState(player: Player): LocalizedComponent {
        val game = GeckoGameManager.findGame(player.uuid) ?: return translatable("display.tablist.lobby")
        return game.settings.map.displayName
    }
}