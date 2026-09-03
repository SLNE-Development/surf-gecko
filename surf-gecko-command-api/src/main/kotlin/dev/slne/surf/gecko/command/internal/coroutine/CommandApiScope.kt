package dev.slne.surf.gecko.command.internal.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import net.minestom.server.MinecraftServer

/**
 * The scope suspending executors and suspending suggestion providers run in unless the caller
 * passes one of its own.
 *
 * A supervisor, so one failing executor never cancels the scope; the failure is reported through
 * Minestom's logger instead. Nothing in this module needs the tick-bound dispatcher, so only the
 * asynchronous scope is provided here.
 *
 * Every entry point that reads this takes the scope as a parameter with this as its default, so a
 * host that already owns a Minestom coroutine scope can hand in its own rather than let a second
 * supervisor exist alongside it.
 */
private object CommandApiScopeProvider {

    val asyncScope: CoroutineScope

    init {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            MinecraftServer.LOGGER.error("Coroutine exception in context: $context", throwable)
        }

        asyncScope = CoroutineScope(exceptionHandler) + SupervisorJob() + Dispatchers.Default
    }
}

val minestomAsyncScope: CoroutineScope get() = CommandApiScopeProvider.asyncScope
