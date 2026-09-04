package dev.slne.surf.gecko.server.gecko.state

enum class GeckoGameState {
    OFFLINE,
    PREPARING,
    LOBBY,
    STARTING,
    HIDING,
    SEARCHING,
    ENDING,
    ENDED;

    fun acceptsPlayers() = this in listOf(LOBBY, STARTING)
    fun isGame() = this in listOf(SEARCHING, HIDING)
}