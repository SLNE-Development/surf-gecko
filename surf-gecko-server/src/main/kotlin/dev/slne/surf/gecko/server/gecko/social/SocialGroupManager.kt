package dev.slne.surf.gecko.server.gecko.social

import com.bradenkennedy.tab.TabVisibilityManager
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.geckoLogger
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*

object SocialGroupManager {
    private val groups = mutableMapOf<UUID, SocialGroup>()
    private val visibilityManager = TabVisibilityManager(MinecraftServer.getGlobalEventHandler())

    fun update(gamePlayer: GeckoGamePlayer) = when (gamePlayer.role) {
        GeckoGameRole.SEEKER -> internalUpdate(gamePlayer.playerUuid, SocialGroup.GAME_SEEKER)
        GeckoGameRole.HIDER -> internalUpdate(gamePlayer.playerUuid, SocialGroup.GAME_HIDER)
        GeckoGameRole.SPECTATOR -> internalUpdate(gamePlayer.playerUuid, SocialGroup.GAME_SPECTATOR)
    }

    fun showAll(game: GeckoGame) {
        game.gamePlayers.forEach { player ->
            internalUpdate(player.playerUuid, SocialGroup.GAME_ALL)
        }
    }

    fun update(lobbyPlayer: GeckoLobbyPlayer) =
        internalUpdate(lobbyPlayer.playerUuid, SocialGroup.GAME_ALL)

    fun showLobby(player: Player) = groups.put(player.uuid, SocialGroup.LOBBY)
    fun setWatcher(player: Player) = groups.put(player.uuid, SocialGroup.WATCHER)

    fun invalidate(playerUuid: UUID) = groups.remove(playerUuid)

    private fun internalUpdate(playerUuid: UUID, socialGroup: SocialGroup) {
        groups[playerUuid] = socialGroup

        val player =
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid) ?: return

        visibilityManager.setGroup(player, socialGroup.tabGroup)
    }

    fun canSee(playerUuid: UUID, targetUuid: UUID): Boolean {
        val playerGroup = groups[playerUuid] ?: return failAndReturn(playerUuid)
        val targetGroup = groups[targetUuid] ?: return failAndReturn(targetUuid)

        return playerGroup == targetGroup
    }

    private fun failAndReturn(playerUuid: UUID): Boolean {
        geckoLogger.warn("Failed to find visibility group for player with UUID: $playerUuid, chat messages may not be delivered correctly!")
        return false
    }
}