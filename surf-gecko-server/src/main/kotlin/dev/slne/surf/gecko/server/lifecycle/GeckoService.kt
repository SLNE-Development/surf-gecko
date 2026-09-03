package dev.slne.surf.gecko.server.lifecycle

/** A core component started before the extensions and stopped after them. */
interface GeckoService {

    val serviceName: String get() = javaClass.simpleName

    suspend fun start()

    suspend fun stop() = Unit
}
