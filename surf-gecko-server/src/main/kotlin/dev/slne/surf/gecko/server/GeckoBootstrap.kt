package dev.slne.surf.gecko.server

import dev.slne.surf.api.minestom.inventory.framework.register
import dev.slne.surf.api.minestom.server.chat.withSignedChat
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTasks
import dev.slne.surf.api.minestom.server.console.withConsole
import dev.slne.surf.api.minestom.server.luckperms.withLuckPerms
import dev.slne.surf.api.minestom.server.npc.withNpcLib
import dev.slne.surf.api.minestom.server.plugins.withPlugins
import dev.slne.surf.api.minestom.server.spark.withSpark
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.gecko.server.chat.GeckoChatListener
import dev.slne.surf.gecko.server.combat.BowCombatListener
import dev.slne.surf.gecko.server.combat.MeleeCombatListener
import dev.slne.surf.gecko.server.command.ServerGeckoCommandRegistrar
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.config.ConfigLoader
import dev.slne.surf.gecko.server.gecko.GeckoGameJoinService
import dev.slne.surf.gecko.server.gecko.death.GeckoDamageListener
import dev.slne.surf.gecko.server.gecko.display.GeckoDisplayListener
import dev.slne.surf.gecko.server.gecko.hotbar.GeckoHotbarListener
import dev.slne.surf.gecko.server.gecko.lobby.listener.GeckoLobbyListener
import dev.slne.surf.gecko.server.gecko.lobby.view.geckoGamesView
import dev.slne.surf.gecko.server.gecko.orbs.GeckoOrbListener
import dev.slne.surf.gecko.server.gecko.player.listener.GeckoPlayerListener
import dev.slne.surf.gecko.server.gecko.shop.ShopItemListener
import dev.slne.surf.gecko.server.gecko.shop.type.shops.hiderShopView
import dev.slne.surf.gecko.server.gecko.shop.type.shops.seekerShopView
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.view.languageView
import dev.slne.surf.gecko.server.performance.EntityTickFilter
import dev.slne.surf.gecko.server.performance.monitor.StatusBarService
import dev.slne.surf.gecko.server.player.LoadLanguageTask
import dev.slne.surf.gecko.server.player.PlayerConnectionService
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.EntityTypeKeys
import kotlin.io.path.Path

val bootstrapLogger: ComponentLogger = ComponentLogger.logger("GeckoBootstrap")

object GeckoBootstrap {

    fun boot() {
        val startupStartedAt = System.nanoTime()

        bootstrapLogger.info("Booting server...")

        val config = ConfigLoader(Path("config.yml")).load()

        config.applyTickDispatcherThreads()
        config.applyChunkViewDistance()
        applyKeepAliveDelay()

        val minecraftServer = initMinecraftServer(config)

        EntityTickFilter.configure(EntityTypeKeys.ARMOR_STAND.key())

        startSurfApi(config)

        runBlocking {
            GeckoTranslations.configure(config.translations)
            GeckoTranslations.reload()
        }

        registerViews()
        ServerGeckoCommandRegistrar.registerAll()
        GeckoChatListener.register()
        StatusBarService.start()

        runBlocking {
            GeckoServer.start(minecraftServer, config, startupStartedAt)
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

    private fun startSurfApi(config: Config) {
        surfMinestomServer {
            maxPlayers = config.maxPlayers

            withConfigurationPhase {
                after(ConfigurationTasks.AWAIT_SETTINGS, LoadLanguageTask.ID, LoadLanguageTask)
            }

            withLuckPerms()
            withSignedChat {
                enforceSecureProfile = config.chat.enforceSecureProfile
                chatSpamThresholdSeconds = config.chat.chatSpamThresholdSeconds
                commandSpamThresholdSeconds = config.chat.commandSpamThresholdSeconds
            }
            withSpark {
                profileOnStartup = config.performance.spark.profileOnStartup
            }
            withNpcLib()
            withConsole {
                threadName = "surf-gecko-console"
                onShutdown = { GeckoServer.shutdownAndExit("console") }
            }
            withPlugins()

            listeners(
                PlayerConnectionService,
                StatusBarService,
                GeckoGameJoinService(),
                GeckoPlayerListener(),
                MeleeCombatListener(),
                BowCombatListener(),
                GeckoDamageListener(),
                GeckoLobbyListener(),
                ShopItemListener(),
                GeckoHotbarListener(),
                GeckoOrbListener(),
                GeckoDisplayListener(),
            )
        }
    }

    private fun registerViews() {
        seekerShopView.register()
        hiderShopView.register()
        geckoGamesView.register()
        languageView.register()
    }

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

    private fun Config.applyChunkViewDistance() {
        val existing = System.getProperty("minestom.chunk-view-distance")
        if (existing != null) {
            bootstrapLogger.info(
                "Chunk view distance pinned via -Dminestom.chunk-view-distance={}; keeping it.",
                existing
            )
            return
        }

        System.setProperty("minestom.chunk-view-distance", performance.viewDistance.toString())
        bootstrapLogger.info("Using a chunk view distance of {} chunks.", performance.viewDistance)
    }
}
