package dev.slne.surf.gecko.server

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.console.GeckoConsole
import dev.slne.surf.gecko.server.gecko.GeckoInstance
import dev.slne.surf.gecko.server.lifecycle.ServerLifecycle
import dev.slne.surf.gecko.server.plugin.MinestomPluginManager
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

@Singleton
class GeckoServer @Inject constructor(
    private val minecraftServer: MinecraftServer,
    private val config: Config,
    private val serverLifecycle: ServerLifecycle,
    private val pluginManager: MinestomPluginManager,
) {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    private var consoleThread: Thread? = null

    suspend fun start(startupStartedAt: Long) {
        check(started.compareAndSet(false, true)) {
            "Gecko server has already been started"
        }

        try {
            LOGGER.info("Initializing core server components.")
            serverLifecycle.start()
            LOGGER.info("Core server components initialized.")

            LOGGER.info("Starting server plugins.")
            pluginManager.startAll()
            LOGGER.info("Server plugins started.")

            installShutdownHook()

            LOGGER.info(
                "Binding server to {}:{}.",
                config.address.host,
                config.address.port,
            )
            minecraftServer.start(config.address.host, config.address.port)

            startConsole()

            GeckoInstance.enable()

            val startupDuration =
                (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

            LOGGER.info(
                "Surf gecko is ready in {}.",
                startupDuration,
            )
        } catch (startupFailure: Throwable) {
            LOGGER.error(
                "Failed to start Surf gecko server.",
                startupFailure,
            )

            runCatching {
                LOGGER.info("Stopping plugins after failed startup.")
                pluginManager.stopAll()
            }.onFailure { failure ->
                startupFailure.addSuppressed(failure)
                LOGGER.error("Failed to stop plugins after startup failure.", failure)
            }

            runCatching {
                LOGGER.info("Shutting down core components after failed startup.")
                serverLifecycle.stop()
            }.onFailure { failure ->
                startupFailure.addSuppressed(failure)
                LOGGER.error("Failed to shut down core components after startup failure.", failure)
            }

            throw startupFailure
        }
    }

    private fun startConsole() {
        val console = GeckoConsole {
            shutdownAndExit("console")
        }

        consoleThread = Thread(
            console::start,
            "surf-gecko-console",
        ).apply {
            isDaemon = true
            start()
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
    private fun shutdownAndExit(source: String) {
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

        if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
            try {
                MinecraftServer.stopCleanly()
            } catch (currentFailure: Throwable) {
                LOGGER.error("Failed to stop the gecko server cleanly.", currentFailure)
                failure = failure.alsoSuppress(currentFailure)
            }
        }

        try {
            LOGGER.info("Stopping server plugins.")
            pluginManager.stopAll()
            LOGGER.info("Server plugins stopped.")
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to stop server plugins.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
        }

        try {
            LOGGER.info("Shutting down core server components.")
            serverLifecycle.stop()
            LOGGER.info("Core server components shut down.")
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to shut down core server components.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
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
