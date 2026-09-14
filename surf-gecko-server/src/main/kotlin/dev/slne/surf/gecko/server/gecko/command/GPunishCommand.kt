package dev.slne.surf.gecko.server.gecko.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.surf.gecko.server.permission.PermissionList

fun gPunishCommand() = commandTree("gpunish") {
    withPermission(PermissionList.COMMAND_PUNISH)

    literalArgument("ban") {

    }

    literalArgument("unban") {

    }

    literalArgument("check") {

    }
}