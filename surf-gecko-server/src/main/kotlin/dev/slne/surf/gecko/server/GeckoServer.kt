package dev.slne.surf.gecko.server

import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.gecko.GeckoInstance
import dev.slne.surf.gecko.server.performance.monitor.StatusBarService
import dev.slne.surf.gecko.server.redis.RedisService
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

object GeckoServer {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    suspend fun start(minecraftServer: MinecraftServer, config: Config, startupStartedAt: Long) {
        check(started.compareAndSet(false, true)) {
            "Gecko server has already been started"
        }

        try {
            installShutdownHook()

            LOGGER.info(
                "Binding server to {}:{}.",
                config.address.host,
                config.address.port,
            )
            minecraftServer.start(config.address.host, config.address.port)

            RedisService.connect()
            GeckoInstance.enable()

            val startupDuration =
                (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

            LOGGER.info(
                "surf-gecko is ready in {}.",
                startupDuration,
            )
        } catch (startupFailure: Throwable) {
            LOGGER.error(
                "Failed to start surf-gecko server.",
                startupFailure,
            )

            runCatching { stop() }.onFailure { failure ->
                startupFailure.addSuppressed(failure)
                LOGGER.error("Failed to stop surf-gecko after startup failure.", failure)
            }

            throw startupFailure
        }
    }

    fun beginShutdown() {
        if (stopped.get()) {
            return
        }

        thread(isDaemon = false, name = "shutdown-thread") {
            shutdownAndExit("command")
        }
    }

    @Blocking
    fun shutdownAndExit(source: String) {
        runBlocking {
            runCatching {
                stop()
            }.onFailure {
                LOGGER.error(
                    "Failed to stop Surf gecko server from {}.",
                    source,
                    it,
                )
            }
        }

        exitProcess(0)
    }

    suspend fun stop() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        LOGGER.info("Stopping Surf gecko.")

        var failure: Throwable? = null

        try {
            GeckoInstance.shutdown()
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to stop the gecko games.", currentFailure)
            failure = currentFailure
        }

        try {
            RedisService.disconnect()
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to disconnect from redis.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
        }

        try {
            StatusBarService.stop()
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to stop the status bar.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
        }

        if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
            try {
                MinecraftServer.stopCleanly()
            } catch (currentFailure: Throwable) {
                LOGGER.error("Failed to stop the gecko server cleanly.", currentFailure)
                failure = failure.alsoSuppress(currentFailure)
            }
        }

        if (failure == null) {
            LOGGER.info("Surf gecko stopped successfully.")
        } else {
            throw failure
        }
    }

    private fun Throwable?.alsoSuppress(next: Throwable): Throwable {
        if (this == null) return next
        addSuppressed(next)
        return this
    }

    private fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    runBlocking {
                        runCatching {
                            stop()
                        }.onFailure(Throwable::printStackTrace)
                    }
                },
                "surf-gecko-shutdown",
            )
        )
    }
}
