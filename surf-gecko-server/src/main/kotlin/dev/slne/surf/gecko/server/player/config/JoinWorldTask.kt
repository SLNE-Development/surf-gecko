package dev.slne.surf.gecko.server.player.config

import net.minestom.server.network.packet.server.configuration.FinishConfigurationPacket

object JoinWorldTask : ConfigurationTask {

    @Suppress("UnstableApiUsage")
    override fun run(context: ConfigurationContext) {
        val player = context.player

        GeckoConfiguration.stopKeepAlive(player)
        player.setPendingOptions(context.spawningInstance, context.event.isHardcore)
        player.sendPacket(FinishConfigurationPacket())
    }
}
