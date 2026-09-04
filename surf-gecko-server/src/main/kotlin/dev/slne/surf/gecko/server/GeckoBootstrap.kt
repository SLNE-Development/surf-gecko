package dev.slne.surf.gecko.server

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.config.ConfigLoader
import dev.slne.surf.gecko.server.di.GeckoServerModule
import dev.slne.surf.gecko.server.performance.EntityTickFilter
import dev.slne.surf.gecko.server.plugin.MinestomPluginLoader
import dev.slne.surf.gecko.server.plugin.PluginCatalog
import dev.slne.surf.gecko.server.plugin.PluginModule
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.EntityTypeKeys
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

val bootstrapLogger: ComponentLogger = ComponentLogger.logger("GeckoBootstrap")

object GeckoBootstrap {
    private val CONFIG_PATH = Path("config.yml")
    private val PLUGINS_PATH = Path("plugins")

    lateinit var injector: Injector

    fun boot() {
        val startupStartedAt = System.nanoTime()

        bootstrapLogger.info("Booting server...")

        val config = ConfigLoader(CONFIG_PATH).load()
        val minecraftServer = initMinecraftServer(config)

        config.applyTickDispatcherThreads()
        applyKeepAliveDelay()
        EntityTickFilter.configure(EntityTypeKeys.ARMOR_STAND.key())

        val pluginCatalog = discoverPlugins()
        val injector = createInjector(config, minecraftServer, pluginCatalog)
        GeckoBootstrap.injector = injector

        runBlocking {
            injector.getInstance(GeckoServer::class.java).start(startupStartedAt)
        }
    }

    private fun initMinecraftServer(config: Config): MinecraftServer {
        bootstrapLogger.info(
            "Initializing gecko server for {}:{}.",
            config.address.host,
            config.address.port,
        )

        val minecraftServer = MinecraftServer.init(config.velocity.createAuth())

        MinecraftServer.setCompressionThreshold(0)
        return minecraftServer
    }

    private fun discoverPlugins(): PluginCatalog {
        MinecraftServer.LOGGER.info("Discovering server plugins.")

        val catalog = PluginCatalog(MinestomPluginLoader.discover())

        MinecraftServer.LOGGER.info(
            "Discovered {} server plugin(s): {}.",
            catalog.plugins.size,
            catalog.plugins.joinToString { plugin -> plugin.meta.id },
        )

        return catalog
    }

    private fun createInjector(
        config: Config,
        minecraftServer: MinecraftServer,
        pluginCatalog: PluginCatalog,
    ): Injector {
        MinecraftServer.LOGGER.info("Creating dependency injector.")

        val modules = buildList<Module> {
            add(GeckoServerModule(config, minecraftServer, pluginCatalog))

            for (plugin in pluginCatalog.plugins) {
                add(PluginModule(plugin, createDataDirectory(plugin)))
            }
        }

        return Guice.createInjector(Stage.PRODUCTION, modules)
    }

    private fun createDataDirectory(plugin: MinestomPlugin): Path =
        PLUGINS_PATH.resolve(plugin.meta.id).createDirectories()

    private const val DISPATCHER_THREADS_PROPERTY = "minestom.dispatcher-threads"
    private const val KEEP_ALIVE_DELAY_PROPERTY = "minestom.keep-alive-delay"
    private const val KEEP_ALIVE_DELAY_MILLIS = 2_000L

    private fun applyKeepAliveDelay() {
        val existing = System.getProperty(KEEP_ALIVE_DELAY_PROPERTY)
        if (existing != null) {
            bootstrapLogger.info(
                "Keep alive delay pinned via -D{}={}; keeping it.",
                KEEP_ALIVE_DELAY_PROPERTY,
                existing
            )
            return
        }

        System.setProperty(KEEP_ALIVE_DELAY_PROPERTY, KEEP_ALIVE_DELAY_MILLIS.toString())
        bootstrapLogger.info("Sending keep alives every {}ms.", KEEP_ALIVE_DELAY_MILLIS)
    }

    private fun Config.applyTickDispatcherThreads() {
        val existing = System.getProperty(DISPATCHER_THREADS_PROPERTY)
        if (existing != null) {
            bootstrapLogger.info(
                "Tick dispatcher threads pinned via -D{}={}; keeping it.",
                DISPATCHER_THREADS_PROPERTY,
                existing
            )
            return
        }

        val threads =
            if (performance.tickThreads <= 0) Runtime.getRuntime()
                .availableProcessors() else performance.tickThreads

        System.setProperty(DISPATCHER_THREADS_PROPERTY, threads.toString())
        bootstrapLogger.info("Using {} tick dispatcher thread(s).", threads)
    }
}
