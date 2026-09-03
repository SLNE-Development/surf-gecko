package dev.slne.surf.gecko.server.gecko.map

import kotlinx.coroutines.coroutineScope
import net.hollowcube.polar.PolarLoader
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.InstanceContainer
import kotlin.io.path.Path

object GeckoMapManager {
    suspend fun prepareMap(map: GeckoMap): InstanceContainer = coroutineScope {
        val map = MinecraftServer.getInstanceManager()
            .createInstanceContainer(PolarLoader(Path("maps/${map.mapName}")))

        map.chunks.forEach {
            map.loadChunk(it.toPosition())
        }

        map
    }
}