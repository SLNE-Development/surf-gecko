package dev.slne.surf.gecko.server.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.api.di.setBinder
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.surf.gecko.command.platform.MinestomCommandOwnership
import dev.slne.surf.gecko.server.chat.GeckoChatService
import dev.slne.surf.gecko.server.command.GeckoCommandApiService
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.gecko.GeckoGameJoinService
import dev.slne.surf.gecko.server.player.PlayerConnectionService
import dev.slne.surf.gecko.server.player.PlayerLimitService
import dev.slne.surf.gecko.server.plugin.PluginCatalog
import net.minestom.server.MinecraftServer
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.instance.InstanceManager

/**
 * The bindings the server itself owns - everything an extension may inject that does not come out
 * of its own module.
 */
class GeckoServerModule(
    private val config: Config,
    private val minecraftServer: MinecraftServer,
    private val pluginCatalog: PluginCatalog,
) : AbstractModule() {

    override fun configure() {
        bind(Config::class.java).toInstance(config)
        bind(PluginCatalog::class.java).toInstance(pluginCatalog)

        bind(MinecraftServer::class.java).toInstance(minecraftServer)
        bind(GlobalEventHandler::class.java).toInstance(MinecraftServer.getGlobalEventHandler())
        bind(InstanceManager::class.java).toInstance(MinecraftServer.getInstanceManager())
        bind(MinestomCommandOwnership::class.java).`in`(Singleton::class.java)
        bind(PlayerLimit::class.java).to(PlayerLimitService::class.java)

        binder().bindEventRegistrar<PlayerLimitService>()
        binder().bindEventRegistrar<PlayerConnectionService>()
        binder().bindEventRegistrar<GeckoCommandApiService>()
        binder().bindEventRegistrar<GeckoChatService>()
        binder().bindEventRegistrar<GeckoGameJoinService>()

        binder().setBinder<EventRegistrar>()
        binder().setBinder<CommandRegistrar>()
    }

    @Provides
    fun sparkConfig(config: Config): Config.SparkConfig = config.performance.spark
}
