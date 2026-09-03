package dev.slne.surf.gecko.server.player.config

import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import org.jetbrains.annotations.Blocking

interface ConfigurationTask {

    val taskName: String get() = javaClass.simpleName

    @Blocking
    fun run(context: ConfigurationContext)
}

class ConfigurationContext(
    val player: Player,
    val isFirstConfig: Boolean,
    val event: AsyncPlayerConfigurationEvent,
    val spawningInstance: Instance,
)
