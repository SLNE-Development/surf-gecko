package dev.slne.surf.gecko.server

import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.config.ConfigLoader
import dev.slne.surf.gecko.server.performance.EntityTickFilter
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
        val minecraftServer = initMinecraftServer(config)

        config.applyTickDispatcherThreads()
        applyKeepAliveDelay()
        EntityTickFilter.configure(EntityTypeKeys.ARMOR_STAND.key())

        runBlocking {
            GeckoServer(minecraftServer, config).start(startupStartedAt)
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