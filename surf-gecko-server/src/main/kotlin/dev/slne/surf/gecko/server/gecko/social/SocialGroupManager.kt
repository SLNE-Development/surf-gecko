package dev.slne.surf.gecko.server.gecko.social

import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.geckoLogger
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import dev.slne.surf.gecko.server.gecko.social.visibility.VisibilityManager
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*

object SocialGroupManager {
    private val groups = mutableMapOf<UUID, SocialGroup>()
    private val visibilityManager = VisibilityManager(MinecraftServer.getGlobalEventHandler())

    fun groups(): Map<UUID, SocialGroup> = groups.toMap()

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

    fun showLobby(player: Player) = internalUpdate(player.uuid, SocialGroup.LOBBY)
    fun setWatcher(player: Player) = internalUpdate(player.uuid, SocialGroup.WATCHER)

    fun invalidate(playerUuid: UUID) = groups.remove(playerUuid)

    private fun internalUpdate(playerUuid: UUID, socialGroup: SocialGroup) {
        groups[playerUuid] = socialGroup

        val player =
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid) ?: return

        visibilityManager.setGroup(player, socialGroup.tabGroup)
    }

    fun canSee(playerUuid: UUID, targetUuid: UUID): Boolean {
        if (playerUuid == targetUuid) return true

        val playerGroup = groups[playerUuid] ?: return failAndReturn(playerUuid)
        val targetGroup = groups[targetUuid] ?: return failAndReturn(targetUuid)

        if (playerGroup == SocialGroup.WATCHER) return true
        if (playerGroup != targetGroup) return false
        if (!playerGroup.gameScoped) return true

        val playerGame = GeckoGameManager.findGame(playerUuid) ?: return false
        val targetGame = GeckoGameManager.findGame(targetUuid) ?: return false

        return playerGame.internalId == targetGame.internalId
    }

    private fun failAndReturn(playerUuid: UUID): Boolean {
        geckoLogger.warn("Failed to find visibility group for player with UUID: $playerUuid, chat messages may not be delivered correctly!")
        return false
    }
}
