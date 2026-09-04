package dev.slne.surf.gecko.server.permission

object PermissionList {
    private const val BASE = "surf.gecko"
    private const val BASE_SERVER = "$BASE.server"
    private const val BASE_GAME = "$BASE.game"

    const val COMMAND_STOP = "$BASE_SERVER.stop.command"
    const val COMMAND_GAMEMODE = "$BASE_GAME.gamemode.command"
    const val COMMAND_GECKO = "$BASE_GAME.gecko.command"
}