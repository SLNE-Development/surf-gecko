package dev.slne.surf.gecko.server.gecko.social.visibility

import net.minestom.server.entity.Player

interface VisibilityGroup {

    fun canSee(observer: Player, target: Player): Boolean

    fun canBeSeenBy(target: Player, observer: Player): Boolean = true
}
