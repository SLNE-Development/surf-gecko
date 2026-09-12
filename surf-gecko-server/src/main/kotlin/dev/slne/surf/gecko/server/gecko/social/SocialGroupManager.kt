package dev.slne.surf.gecko.server.gecko.social

import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.display.tablist.GeckoTablistRenderer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGamePlayer
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.player.lobby.GeckoLobbyPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*

object SocialGroupManager {

    fun groups(): Map<UUID, SocialGroup> = MinecraftServer.getConnectionManager()
        .onlinePlayers
        .associate { it.uuid to groupOf(it.uuid) }

    fun groupOf(playerUuid: UUID): SocialGroup {
        val game = GeckoGameManager.findGame(playerUuid) ?: return SocialGroup.LOBBY

        if (!game.state.isGame()) {
            return SocialGroup.GAME_ALL
        }

        return when (game.findGamePlayer(playerUuid)?.role) {
            GeckoGameRole.SEEKER -> SocialGroup.GAME_SEEKER
            GeckoGameRole.HIDER -> SocialGroup.GAME_HIDER
            GeckoGameRole.SPECTATOR -> SocialGroup.GAME_SPECTATOR
            null -> SocialGroup.GAME_ALL
        }
    }

    fun gameOf(playerUuid: UUID) = GeckoGameManager.findGame(playerUuid)?.internalId

    fun canChat(playerUuid: UUID, targetUuid: UUID): Boolean {
        if (playerUuid == targetUuid) return true

        val group = groupOf(playerUuid)
        if (group != groupOf(targetUuid)) return false
        if (!group.gameScoped) return true

        return gameOf(playerUuid) == gameOf(targetUuid)
    }

    fun update(gamePlayer: GeckoGamePlayer) = GeckoTablistRenderer.refresh()

    fun update(lobbyPlayer: GeckoLobbyPlayer) = GeckoTablistRenderer.refresh()

    fun showAll(game: GeckoGame) = GeckoTablistRenderer.refresh()

    fun showLobby(player: Player) = GeckoTablistRenderer.refresh()

    fun invalidate(playerUuid: UUID) = GeckoTablistRenderer.reset(playerUuid)
}
