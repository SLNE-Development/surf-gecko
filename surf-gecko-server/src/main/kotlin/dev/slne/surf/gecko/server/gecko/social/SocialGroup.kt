package dev.slne.surf.gecko.server.gecko.social

import com.bradenkennedy.tab.api.VisibilityGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.SameGameGroup

enum class SocialGroup(
    val displayName: String,
    val tabGroup: VisibilityGroup,
    val gameScoped: Boolean = false
) {
    WATCHER("Beobachter", VisibilityGroup.GLOBAL),
    LOBBY("Lobbymitglied", VisibilityGroup.PER_INSTANCE),
    GAME_ALL("Spielmitspieler", SameGameGroup, gameScoped = true),
    GAME_HIDER("Spieler (Hider)", SameGameGroup, gameScoped = true),
    GAME_SEEKER("Spieler (Seeker)", SameGameGroup, gameScoped = true),
    GAME_SPECTATOR("Spieler (Spectator)", SameGameGroup, gameScoped = true)
}
