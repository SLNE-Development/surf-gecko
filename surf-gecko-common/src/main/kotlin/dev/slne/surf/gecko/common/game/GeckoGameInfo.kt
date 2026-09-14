package dev.slne.surf.gecko.common.game

data class GeckoGameInfo(
    val gameId: ULong,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val mapDisplayName: String,
) {
    override fun toString(): String {
        return "GeckoGameInfo(gameId=$gameId, currentPlayers=$currentPlayers, maxPlayers=$maxPlayers, mapDisplayName='$mapDisplayName')"
    }
}
