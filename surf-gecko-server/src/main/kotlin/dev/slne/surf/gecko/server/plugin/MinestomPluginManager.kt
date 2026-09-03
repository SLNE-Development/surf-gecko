package dev.slne.surf.gecko.server.plugin

import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import net.minestom.server.MinecraftServer.LOGGER

/**
 * Runs the extensions' `start` / `afterStart` / `stop` lifecycle.
 *
 * Every extension is started in [PluginCatalog] order; `afterStart` only runs once all of them
 * are up, so an extension may reach for another's service there. A failure during startup rolls
 * the already-started extensions back before it is rethrown, and shutdown runs in reverse order.
 */
@Singleton
class MinestomPluginManager @Inject constructor(
    private val injector: Injector,
    private val catalog: PluginCatalog,
) {
    private data class StartedPlugin(
        val plugin: MinestomPlugin,
        val entrypoint: MinestomPluginEntrypoint,
    )

    private val startedPlugins = ArrayDeque<StartedPlugin>()

    private var started = false

    suspend fun startAll() {
        check(!started) { "Minestom plugins have already been started" }

        try {
            for (plugin in catalog.plugins) {
                LOGGER.debug("Starting plugin {}.", plugin.meta.id)

                val entrypoint = injector.getInstance(plugin.entrypoint)

                entrypoint.start()

                startedPlugins.addLast(StartedPlugin(plugin, entrypoint))
            }

            for ((plugin, entrypoint) in startedPlugins) {
                LOGGER.debug("Running afterStart for plugin {}.", plugin.meta.id)
                entrypoint.afterStart()
            }

            started = true
        } catch (startupFailure: Throwable) {
            rollback(startupFailure)
            throw startupFailure
        }
    }

    suspend fun stopAll() {
        var failure: Throwable? = null

        while (startedPlugins.isNotEmpty()) {
            val startedPlugin = startedPlugins.removeLast()

            LOGGER.debug("Stopping plugin {}.", startedPlugin.plugin.meta.id)

            try {
                startedPlugin.entrypoint.stop()
            } catch (currentFailure: Throwable) {
                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        started = false

        failure?.let { throw it }
    }

    private suspend fun rollback(startupFailure: Throwable) {
        while (startedPlugins.isNotEmpty()) {
            val startedPlugin = startedPlugins.removeLast()

            try {
                startedPlugin.entrypoint.stop()
            } catch (rollbackFailure: Throwable) {
                startupFailure.addSuppressed(rollbackFailure)
            }
        }
    }
}
