package dev.slne.surf.gecko.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.surf.gecko.command.platform.CommandAPIHook
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import net.minestom.server.MinecraftServer
import net.minestom.server.utils.callback.CommandCallback

@Singleton
class GeckoCommandService @Inject constructor(
    private val registrars: Set<@JvmSuppressWildcards CommandRegistrar>,
) : GeckoService {

    override suspend fun start() {
        ServerGeckoCommandRegistrar.registerAll()

        for (registrar in registrars) {
            registrar.register()
        }

        MinecraftServer.getCommandManager().unknownCommandCallback =
            CommandCallback(CommandAPIHook::reportUnknown)
    }
}
