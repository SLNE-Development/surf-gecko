package dev.slne.surf.gecko.server.gecko.social

import dev.slne.surf.gecko.server.gecko.social.visibility.GlobalGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.PerInstanceGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.SameGameGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.VisibilityGroup

enum class SocialGroup(
    val displayName: String,
    val tabGroup: VisibilityGroup,
    val gameScoped: Boolean = false
) {
    WATCHER("Beobachter", GlobalGroup),
    LOBBY("Lobbymitglied", PerInstanceGroup),
    GAME_ALL("Spielmitspieler", SameGameGroup, gameScoped = true),
    GAME_HIDER("Spieler (Hider)", SameGameGroup, gameScoped = true),
    GAME_SEEKER("Spieler (Seeker)", SameGameGroup, gameScoped = true),
    GAME_SPECTATOR("Spieler (Spectator)", SameGameGroup, gameScoped = true)
}
