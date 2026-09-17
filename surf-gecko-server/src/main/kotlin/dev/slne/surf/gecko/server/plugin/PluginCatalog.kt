package dev.slne.surf.gecko.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin

class PluginCatalog(discoveredPlugins: Collection<MinestomPlugin>) {
    val plugins: List<MinestomPlugin> = PluginDependencyResolver.resolve(discoveredPlugins)
}
