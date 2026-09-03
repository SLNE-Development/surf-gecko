package dev.slne.surf.gecko.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.command.GeckoCommandApiService
import dev.slne.surf.gecko.server.command.GeckoCommandService
import dev.slne.surf.gecko.server.event.GeckoEventService
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import dev.slne.surf.gecko.server.integration.spark.SparkService
import net.minestom.server.MinecraftServer.LOGGER
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts the core components in a fixed order and stops them in the reverse one.
 *
 * Events go first so every registrar - the server's own and the extensions' - is attached before
 * anything can fire; LuckPerms next because permission checks resolve through it; the command API
 * platform before the commands that register onto it.
 */
@Singleton
class ServerLifecycle @Inject constructor(
    events: GeckoEventService,
    luckPerms: LuckPermsService,
    spark: SparkService,
    commandApi: GeckoCommandApiService,
    commands: GeckoCommandService,
) {
    private val services = listOf<GeckoService>(events, luckPerms, spark, commandApi, commands)

    private val started = ArrayDeque<GeckoService>()
    private val running = AtomicBoolean()

    suspend fun start() {
        check(running.compareAndSet(false, true)) {
            "Core server components have already been started"
        }

        for (service in services) {
            LOGGER.debug("Starting {}.", service.serviceName)

            try {
                service.start()
            } catch (startupFailure: Throwable) {
                runCatching { stop() }.onFailure(startupFailure::addSuppressed)
                throw startupFailure
            }

            started.addLast(service)
        }
    }

    suspend fun stop() {
        if (!running.compareAndSet(true, false)) return

        var failure: Throwable? = null

        while (started.isNotEmpty()) {
            val service = started.removeLast()

            LOGGER.debug("Stopping {}.", service.serviceName)

            try {
                service.stop()
            } catch (currentFailure: Throwable) {
                LOGGER.error("Failed to stop {}.", service.serviceName, currentFailure)

                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        failure?.let { throw it }
    }
}
