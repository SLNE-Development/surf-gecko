package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.permission.PermissionList

fun skipCommand() = commandTree("skip") {
    withPermission(PermissionList.COMMAND_SKIP)

    withRequirement {
        val uuid = it.uuidOrNull() ?: return@withRequirement true
        GeckoGameManager.findGame(uuid)?.takeIf { it.state == GeckoGameState.LOBBY } != null
    }

    playerExecutor { player, _ ->
        val uuid = player.uuidOrNull() ?: return@playerExecutor
        val game = GeckoGameManager.findGame(uuid) ?: return@playerExecutor

        if (game.state != GeckoGameState.LOBBY) {
            return@playerExecutor
        }

        game.countdownSeconds = 10

        game.sendText {
            appendSuccessPrefix()
            success("Der Countdown wurde verkürzt.")
        }

        player.sendText {
            appendSuccessPrefix()
            success("Der Countdown wurde verkürzt.")
        }
    }
}