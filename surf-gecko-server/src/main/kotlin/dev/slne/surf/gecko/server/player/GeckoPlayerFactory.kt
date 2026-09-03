package dev.slne.surf.gecko.server.player

import dev.slne.surf.gecko.server.config.Config
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsService
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

class GeckoPlayerFactory(
    private val luckPermsService: LuckPermsService,
    private val chatConfig: Config.ChatConfig,
) {

    fun create(
        playerConnection: PlayerConnection,
        gameProfile: GameProfile,
    ): GeckoPlayer = GeckoPlayer(playerConnection, gameProfile, luckPermsService, chatConfig)
}
