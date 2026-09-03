package dev.slne.surf.gecko.server.player.config

import net.minestom.server.network.packet.server.configuration.ResetChatPacket
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket

object EnabledFeaturesTask : ConfigurationTask {

    override fun run(context: ConfigurationContext) {
        val event = context.event

        context.player.sendPacket(
            UpdateEnabledFeaturesPacket(event.featureFlags.map { it.name() })
        )

        if (event.willClearChat()) {
            context.player.sendPacket(ResetChatPacket())
        }
    }
}
