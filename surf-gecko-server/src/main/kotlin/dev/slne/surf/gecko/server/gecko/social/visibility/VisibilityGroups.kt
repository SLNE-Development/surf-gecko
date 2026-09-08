package dev.slne.surf.gecko.server.gecko.social.visibility

import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import net.minestom.server.entity.Player

object GlobalGroup : VisibilityGroup {
    override fun canSee(observer: Player, target: Player) = true
}

object PerInstanceGroup : VisibilityGroup {
    override fun canSee(observer: Player, target: Player): Boolean {
        val instance = observer.instance ?: return false

        return instance == target.instance
    }
}

object SameGameGroup : VisibilityGroup {
    override fun canSee(observer: Player, target: Player): Boolean {
        val observerGame = GeckoGameManager.findGame(observer.uuid) ?: return false
        val targetGame = GeckoGameManager.findGame(target.uuid) ?: return false

        return observerGame.internalId == targetGame.internalId
    }
}

class SpectatorGroup(private val visibilityManager: VisibilityManager) : VisibilityGroup {
    override fun canSee(observer: Player, target: Player) = observer.instance == target.instance

    override fun canBeSeenBy(target: Player, observer: Player) =
        visibilityManager.groupOf(observer) is SpectatorGroup
}
