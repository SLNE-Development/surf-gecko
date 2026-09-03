package dev.slne.surf.gecko.server

import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.console.GeckoConsole
import dev.slne.surf.gecko.server.gecko.GeckoInstance
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class GeckoServer(
    private val minecraftServer: MinecraftServer,
    private val config: Config,
) {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    private var consoleThread: Thread? = null

    suspend fun start(startupStartedAt: Long) {
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

            startConsole()

            val startupDuration =
                (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

            LOGGER.info(
                "Surf gecko is ready in {}.",
                startupDuration,
            )
        } catch (startupFailure: Throwable) {
            LOGGER.error(
                "Failed to start Surf gecko.",
                startupFailure,
            )

            throw startupFailure
        }

        GeckoInstance.enable()
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
        if (stopped.get()) return
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

        GeckoInstance.shutdown()

        if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
            try {
                MinecraftServer.stopCleanly()
            } catch (currentFailure: Throwable) {
                LOGGER.error(
                    "Failed to stop the gecko server cleanly.",
                    currentFailure,
                )
                throw currentFailure
            }
        }

        LOGGER.info("Surf gecko stopped successfully.")
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
