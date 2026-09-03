package dev.slne.surf.gecko.server.gecko.state

enum class GeckoGameEndReason {
    MANUELL,
    ERROR,
    SEEKER_WIN,
    HIDER_WIN,
    NO_PLAYERS,
    UNSET,
    SHUTDOWN;

    fun canMovePlayers() = this !in listOf(SHUTDOWN)
}