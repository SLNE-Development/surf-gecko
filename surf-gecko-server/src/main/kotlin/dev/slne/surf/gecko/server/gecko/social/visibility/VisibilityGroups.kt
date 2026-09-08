package dev.slne.surf.gecko.server.gecko.social.visibility

import com.bradenkennedy.tab.api.VisibilityGroup
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.entity.Player

object SameGameGroup : VisibilityGroup {
    override fun canSee(
        observer: Player,
        target: Player
    ): Boolean {
        val gameObserver = GeckoGameManager.findGame(observer.uuid)
        val gameTarget = GeckoGameManager.findGame(target.uuid)

        return !(gameObserver == null || gameTarget == null) && gameObserver.internalId == gameTarget.internalId
    }
}

object SameRoleGroup : VisibilityGroup {
    override fun canSee(
        observer: Player,
        target: Player
    ): Boolean {
        val observerGamePlayer =
            GeckoGameManager.findGame(observer.uuid)?.findGamePlayer(observer.uuid)
        val targetGamePlayer = GeckoGameManager.findGame(target.uuid)?.findGamePlayer(target.uuid)

        return !(observerGamePlayer == null || targetGamePlayer == null) && observerGamePlayer.role == targetGamePlayer.role
    }
}