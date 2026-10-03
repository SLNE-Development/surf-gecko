package dev.slne.surf.gecko.server.i18n

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.lifecycle.GeckoService

@Singleton
class GeckoTranslationService @Inject constructor(
    private val config: Config,
) : GeckoService {
    override suspend fun start() {
        GeckoTranslations.configure(config.translations)
        GeckoTranslations.reload()
    }
}
