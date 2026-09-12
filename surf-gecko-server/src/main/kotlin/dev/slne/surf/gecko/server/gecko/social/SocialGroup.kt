package dev.slne.surf.gecko.server.gecko.social

enum class SocialGroup(
    val displayName: String,
    val gameScoped: Boolean = false
) {
    LOBBY("Lobbymitglied"),
    GAME_ALL("Spielmitspieler", gameScoped = true),
    GAME_HIDER("Spieler (Hider)", gameScoped = true),
    GAME_SEEKER("Spieler (Seeker)", gameScoped = true),
    GAME_SPECTATOR("Spieler (Spectator)", gameScoped = true)
}
