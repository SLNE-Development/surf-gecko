package dev.slne.surf.gecko.server.gecko.display.tablist

import dev.slne.surf.gecko.server.gecko.social.SocialGroup
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

class TabProfile(
    val player: Player,
    val language: GeckoLanguage,
    val group: SocialGroup,
    val gameId: ULong?,
    val rankedName: Component,
    val rankOrder: Int,
    val roleName: Map<GeckoLanguage, Component>?,
    val grayName: Component
)