package dev.slne.surf.gecko.server.gecko.map

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.gecko.server.bootstrapLogger
import dev.slne.surf.gecko.server.event.register
import dev.slne.surf.gecko.server.gecko.map.mechanic.GeckoMapMechanic
import dev.slne.surf.gecko.server.gecko.map.mechanic.VentMechanic
import kotlinx.coroutines.coroutineScope
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.anvil.AnvilLoader
import kotlin.io.path.Path

object GeckoMapManager {
    suspend fun prepareMap(map: GeckoMap): InstanceContainer = coroutineScope {
        val map = MinecraftServer.getInstanceManager()
            .createInstanceContainer(
                AnvilLoader(
                    Path("maps/${map.mapName}"),
                    key("minecraft:overworld")
                )
            )

        map
    }

    private val mechanics = mutableListOf<GeckoMapMechanic>()

    suspend fun registerMechanics() {
        bootstrapLogger.info("Registering map mechanics...")
        registerMechanic(VentMechanic)
        bootstrapLogger.info("Registered ${mechanics.size} map mechanics")
    }

    suspend fun unregisterMechanics() {
        mechanics.forEach {
            it.stop()
        }
    }

    private suspend fun registerMechanic(mechanic: GeckoMapMechanic) {
        mechanics.add(mechanic)

        mechanic.listeners.forEach {
            it.register()
        }

        mechanic.start()

        bootstrapLogger.info("Registered mechanic ${mechanic::class.simpleName}")
    }
}