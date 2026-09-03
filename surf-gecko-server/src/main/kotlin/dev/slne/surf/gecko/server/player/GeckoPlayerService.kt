package dev.slne.surf.gecko.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import dev.slne.surf.gecko.server.player.config.AwaitSettingsTask
import dev.slne.surf.gecko.server.player.config.EnabledFeaturesTask
import dev.slne.surf.gecko.server.player.config.GeckoConfiguration
import dev.slne.surf.gecko.server.player.config.JoinWorldTask
import dev.slne.surf.gecko.server.player.config.ResourcePackTask
import dev.slne.surf.gecko.server.player.config.SynchronizeRegistriesTask

@Singleton
class GeckoPlayerService @Inject constructor(
    config: Config,
    luckPermsService: LuckPermsService,
    playerLimit: PlayerLimit,
) : GeckoService {

    private val playerFactory = GeckoPlayerFactory(luckPermsService, config.chat)
    private val loginGate = PlayerLoginGate(playerLimit)

    override suspend fun start() {
        ConnectionManager.setPlayerProvider(playerFactory::create)

        GeckoConfiguration.install(
            listOf(
                EnabledFeaturesTask,
                SynchronizeRegistriesTask,
                AwaitSettingsTask,
                ResourcePackTask,
                JoinWorldTask,
            ),
            loginGate,
        )
    }
}
