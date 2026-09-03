package dev.slne.surf.gecko.server.gecko.state

enum class GeckoGameState {
    OFFLINE,
    PREPARING,
    LOBBY,
    STARTING,
    GAME,
    ENDING,
    ENDED;

    fun acceptsPlayers() = this in listOf(LOBBY, STARTING)
}