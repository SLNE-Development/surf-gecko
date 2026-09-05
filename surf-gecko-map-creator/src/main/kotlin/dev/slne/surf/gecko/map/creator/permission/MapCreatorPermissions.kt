package dev.slne.surf.gecko.map.creator.permission

import dev.slne.surf.api.paper.permission.PermissionRegistry

object MapCreatorPermissions : PermissionRegistry() {
    val COMMAND = create("gecko.mapcreator.command")
}
