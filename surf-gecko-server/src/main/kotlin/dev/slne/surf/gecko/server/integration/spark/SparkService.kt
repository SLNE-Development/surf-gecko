package dev.slne.surf.gecko.server.integration.spark

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import me.lucko.spark.minestom.MinestomSparkPlugin
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import kotlin.io.path.Path

@Singleton
class SparkService @Inject constructor(
    private val luckPerms: LuckPermsService,
    private val config: Config.SparkConfig,
) : GeckoService {
    private var plugin: MinestomSparkPlugin? = null

    override suspend fun start() {
        val spark = MinestomSparkPlugin(Path("plugins/spark")) { sender, permission ->
            sender !is Player || luckPerms.hasPermission(sender.uuid, permission).asBoolean()
        }

        spark.enable()
        plugin = spark

        if (config.profileOnStartup) {
            val commandManager = MinecraftServer.getCommandManager()
            commandManager.execute(commandManager.consoleSender, "spark profiler start --thread *")
        }
    }

    override suspend fun stop() {
        plugin?.disable()
        plugin = null
    }
}
