package dev.slne.surf.gecko.server.gecko.social

import com.bradenkennedy.tab.api.VisibilityGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.SameGameGroup
import dev.slne.surf.gecko.server.gecko.social.visibility.SameRoleGroup

enum class SocialGroup(val displayName: String, val tabGroup: VisibilityGroup) {
    WATCHER("Beobachter", VisibilityGroup.GLOBAL),
    LOBBY("Lobbymitglied", VisibilityGroup.PER_INSTANCE),
    GAME_ALL("Spielmitspieler", SameGameGroup),
    GAME_HIDER("Spieler (Hider)", SameRoleGroup),
    GAME_SEEKER("Spieler (Seeker)", SameRoleGroup),
    GAME_SPECTATOR("Spieler (Spectator)", SameRoleGroup)
}