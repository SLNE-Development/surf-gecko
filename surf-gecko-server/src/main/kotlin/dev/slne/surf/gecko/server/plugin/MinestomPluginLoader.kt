package dev.slne.surf.gecko.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import java.util.ServiceLoader

/**
 * Finds the extensions on the classpath.
 *
 * Every surf-*-minestom extension ships a `META-INF/services` entry for [MinestomPlugin], so
 * discovery is a plain [ServiceLoader] lookup - there is no plugin directory to scan and no
 * class loader to build. The extension jars are appended to the system class loader by
 * [dev.slne.surf.gecko.server.instrumentation.DependencyInstaller] before `main` runs.
 */
object MinestomPluginLoader {

    fun discover(
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    ): List<MinestomPlugin> = ServiceLoader.load(MinestomPlugin::class.java, classLoader).toList()
}
