package dev.slne.surf.gecko.server.gecko.state

enum class GeckoGameState {
    OFFLINE,
    LOBBY,
    HIDING,
    SEARCHING,
    ENDING,
    ENDED;

    fun acceptsPlayers() = this in listOf(LOBBY)
    fun isGame() = this in listOf(SEARCHING, HIDING)
}