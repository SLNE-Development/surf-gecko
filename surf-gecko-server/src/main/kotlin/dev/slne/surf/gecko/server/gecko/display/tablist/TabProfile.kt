package dev.slne.surf.gecko.server.gecko.display.tablist

import dev.slne.surf.gecko.server.gecko.social.SocialGroup
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

class TabProfile(
    val player: Player,
    val group: SocialGroup,
    val gameId: ULong?,
    val rankedName: Component,
    val rankOrder: Int,
    val roleName: Component?,
    val grayName: Component
)