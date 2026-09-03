package dev.slne.surf.gecko.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin

/** The discovered extensions, in the order they have to be started. */
class PluginCatalog(discoveredPlugins: Collection<MinestomPlugin>) {
    val plugins: List<MinestomPlugin> = PluginDependencyResolver.resolve(discoveredPlugins)
}
