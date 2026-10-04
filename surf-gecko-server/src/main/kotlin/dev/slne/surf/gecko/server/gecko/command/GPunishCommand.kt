package dev.slne.surf.gecko.server.gecko.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.literalArgument
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