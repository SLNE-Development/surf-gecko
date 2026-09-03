package dev.slne.surf.gecko.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.chat.GeckoChatService
import dev.slne.surf.gecko.server.command.GeckoCommandApiService
import dev.slne.surf.gecko.server.command.GeckoCommandService
import dev.slne.surf.gecko.server.event.GeckoEventService
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import dev.slne.surf.gecko.server.integration.spark.SparkService
import dev.slne.surf.gecko.server.player.GeckoPlayerService
import net.minestom.server.MinecraftServer.LOGGER
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class ServerLifecycle @Inject constructor(
    events: GeckoEventService,
    luckPerms: LuckPermsService,
    spark: SparkService,
    commandApi: GeckoCommandApiService,
    commands: GeckoCommandService,
    chat: GeckoChatService,
    players: GeckoPlayerService,
) {
    private val services =
        listOf<GeckoService>(events, luckPerms, spark, commandApi, commands, chat, players)

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
        if (!running.compareAndSet(true, false)) {
            return
        }

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
