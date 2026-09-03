package dev.slne.surf.gecko.server.gecko.settings.map.convert

import dev.slne.surf.gecko.server.bootstrapLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.hollowcube.polar.AnvilPolar
import net.hollowcube.polar.PolarWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

object GeckoMapConverter {
    private val ANVIL_DIR = Path("maps/anvil")
    private val POLAR_DIR = Path("maps/polar")
    private val ANVIL_CONVERTED_DIR = Path("maps/anvil-converted")

    suspend fun convertAll() = withContext(Dispatchers.IO) {
        POLAR_DIR.createDirectories()
        ANVIL_CONVERTED_DIR.createDirectories()

        if (!ANVIL_DIR.isDirectory()) {
            return@withContext
        }

        val anvilMaps = ANVIL_DIR.listDirectoryEntries().filter { it.isDirectory() }

        for (anvilMap in anvilMaps) {
            val mapName = anvilMap.name
            val polarFile = POLAR_DIR.resolve("$mapName.polar")

            if (polarFile.exists()) {
                bootstrapLogger.info("Map '$mapName' already converted, skipping.")
                continue
            }

            try {
                val polarWorld = AnvilPolar.anvilToPolar(anvilMap)
                val polarBytes = PolarWriter.write(polarWorld)

                POLAR_DIR.resolve("$mapName.polar").writeBytes(polarBytes)

                val target = ANVIL_CONVERTED_DIR.resolve(mapName)
                Files.move(anvilMap, target, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                bootstrapLogger.error("Failed to convert map '$mapName': ${e.message}", e)
            }
        }
    }
}