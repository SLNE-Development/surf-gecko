package dev.slne.surf.gecko.server.gecko.tablist

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import kotlinx.coroutines.Job
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import kotlin.time.Duration.Companion.seconds

object GeckoGameTablistManager {
    private lateinit var job: Job

    fun init() {
        job = geckoAsyncScope.runAtFixedRate(1.seconds) {
            sendAdditions()
        }
    }

    fun shutdown() {
        if (::job.isInitialized && job.isActive) {
            job.cancel()
        }
    }

    fun sendAdditions() {
        MinecraftServer.getConnectionManager().onlinePlayers.forEach {
            it.sendPlayerListHeaderAndFooter(buildText {
                appendNewline()
                note("CASTCRAFTER.DE")
                appendNewline()
                info("Hide 'n Seek")
                spacer(" » ")
                info(currentState(it))
                appendNewline()
            }, buildText {
                appendNewline()
                primary("castcrafter.de".toSmallCaps())
                appendNewline()
            })
        }
    }

    private fun currentState(player: Player): String {
        val game = GeckoGameManager.findGame(player.uuid) ?: return "Lobby"
        return game.settings.map.mapDisplayName
    }
}