package dev.slne.surf.gecko.server.player.config

import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import kotlinx.coroutines.runBlocking

object LoadLanguageTask : ConfigurationTask {
    override fun run(context: ConfigurationContext) {
        if (!context.isFirstConfig) return

        runBlocking { PlayerLanguages.load(context.player) }
    }
}
