package dev.slne.surf.gecko.command.platform

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.entity.Player

/**
 * Resolves the permissions a command node requires against LuckPerms.
 *
 * Minestom itself carries no permission model, so this is the one place in the port that names a
 * permission backend. Swapping LuckPerms for something else means changing this file and nothing
 * else: [MinestomConditions] is its only caller.
 *
 * A player whose user is not loaded - and every sender at all while LuckPerms is not running - is
 * denied any permission that is asked for, so a node guarded by one stays hidden and unusable
 * rather than falling open.
 */
internal object MinestomPermissions {

    fun hasPermission(player: Player, permission: String): Boolean {
        val luckPerms = provider() ?: return false
        val user = luckPerms.userManager.getUser(player.uuid) ?: return false

        return user.cachedData.permissionData.checkPermission(permission).asBoolean()
    }

    /**
     * The running LuckPerms instance, or `null` before it is enabled and after it is disabled.
     *
     * Read on every check rather than cached, so a restarted LuckPerms is picked up instead of a
     * stale instance being held on to.
     */
    private fun provider(): LuckPerms? = try {
        LuckPermsProvider.get()
    } catch (_: IllegalStateException) {
        null
    }
}
