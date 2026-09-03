package dev.slne.surf.gecko.server.coroutine

import kotlinx.coroutines.*
import net.minestom.server.MinecraftServer

internal object MinestomScopeProvider {

    val scope: CoroutineScope
    val asyncScope: CoroutineScope
    val blockingScope: CoroutineScope

    init {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            MinecraftServer.LOGGER.error("Coroutine exception in context: $context", throwable)
        }
        val rootScope = CoroutineScope(exceptionHandler)

        scope = rootScope + SupervisorJob() + MinestomDispatchers.Main
        asyncScope = rootScope + SupervisorJob() + Dispatchers.Default
        blockingScope = rootScope + SupervisorJob() + MinestomDispatchers.Blocking
    }
}

val geckoScope get() = MinestomScopeProvider.scope
val geckoAsyncScope get() = MinestomScopeProvider.asyncScope
val geckoBlockingScope get() = MinestomScopeProvider.blockingScope