package dev.slne.surf.gecko.server.gecko.map

import dev.slne.surf.api.core.messages.adventure.key
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
}