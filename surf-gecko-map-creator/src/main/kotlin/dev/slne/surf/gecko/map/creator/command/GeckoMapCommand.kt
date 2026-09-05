package dev.slne.surf.gecko.map.creator.command

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend

fun geckoMapCommand() = commandTree("geckomap") {
    withPermission("geckomap.command")

    literalArgument("create") {
        playerExecutor { player, arguments ->

        }
    }

    literalArgument("export") {
        playerExecutorSuspend { player, arguments ->

        }
    }

    literalArgument("reset") {
        playerExecutor { player, arguments ->

        }
    }
}