package dev.slne.surf.gecko.server.plugin

import com.google.inject.AbstractModule
import com.google.inject.PrivateModule
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.di.setBinder
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.annotation.DataDirectory
import java.nio.file.Path

/**
 * Binds one extension into the server injector.
 *
 * The extension's own module is installed privately, so two extensions binding the same type
 * cannot collide; only the entrypoint and the registrars it declared are exposed. `@DataDirectory`
 * is private per extension too - each one sees its own `plugins/<id>` directory.
 */
class PluginModule(
    private val plugin: MinestomPlugin,
    private val dataDirectory: Path,
) : AbstractModule() {

    override fun configure() {
        install(PluginBindings(plugin, dataDirectory))

        val eventRegistrars = binder().setBinder<EventRegistrar>()
        for (registrar in plugin.eventRegistrars) {
            eventRegistrars.addBinding().to(registrar)
        }

        val commandRegistrars = binder().setBinder<CommandRegistrar>()
        for (registrar in plugin.commandRegistrars) {
            commandRegistrars.addBinding().to(registrar)
        }
    }

    private class PluginBindings(
        private val plugin: MinestomPlugin,
        private val dataDirectory: Path,
    ) : PrivateModule() {

        override fun configure() {
            bind(Path::class.java)
                .annotatedWith(DataDirectory::class.java)
                .toInstance(dataDirectory)

            install(plugin)

            expose(plugin.entrypoint)
            plugin.eventRegistrars.forEach { registrar -> expose(registrar) }
            plugin.commandRegistrars.forEach { registrar -> expose(registrar) }
        }
    }
}
