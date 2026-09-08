package dev.slne.surf.gecko.server.permission

object PermissionList {
    private const val BASE = "surf.gecko"
    private const val BASE_SERVER = "$BASE.server"
    private const val BASE_GAME = "$BASE.game"

    const val COMMAND_STOP = "$BASE_SERVER.stop.command"
    const val COMMAND_TPSBAR = "$BASE_SERVER.tpsbar.command"
    const val COMMAND_TPSBAR_OTHERS = "$BASE_SERVER.tpsbar.others.command"
    const val COMMAND_GAMEMODE = "$BASE_GAME.gamemode.command"
    const val COMMAND_TELEPORT = "$BASE_GAME.teleport.command"
    const val COMMAND_MINIMESSAGE = "$BASE_GAME.minimessage.command"
    const val COMMAND_GECKO = "$BASE_GAME.gecko.command"
    const val COMMAND_SKIP = "$BASE_GAME.skip.command"
    const val COMMAND_LOBBY = "$BASE_GAME.lobby.command"
}