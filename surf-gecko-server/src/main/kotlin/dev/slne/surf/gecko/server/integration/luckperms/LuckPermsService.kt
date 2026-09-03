package dev.slne.surf.gecko.server.integration.luckperms

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.surf.gecko.server.lifecycle.GeckoService
import me.lucko.luckperms.minestom.CommandRegistry
import me.lucko.luckperms.minestom.LuckPermsMinestom
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.util.Tristate
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.div

/**
 * Runs LuckPerms, the permission backend the extensions check against.
 *
 * It comes first in the lifecycle: the command API's permission conditions and the punish and
 * settings extensions all resolve permissions through it, and a check made before it is up is
 * denied rather than falling open.
 */
@Singleton
class LuckPermsService @Inject constructor() : GeckoService {

    private var instance: LuckPerms? = null

    val luckPerms: LuckPerms
        get() = checkNotNull(instance) {
            "LuckPermsService has not been started yet - it has to come first in ServerLifecycle"
        }

    override suspend fun start() {
        instance = LuckPermsMinestom.builder(DATA_DIRECTORY)
            .commandRegistry(CommandRegistry.minestom())
            .configurationAdapter { plugin ->
                LuckPermsConfigAdapter(plugin, DATA_DIRECTORY / "config.yml")
            }
            .enable()
    }

    override suspend fun stop() {
        if (instance == null) return

        instance = null
        LuckPermsMinestom.disable()
    }

    fun hasPermission(uuid: UUID, permission: String): Tristate {
        val user = getLoadedUser(uuid) ?: return Tristate.FALSE
        return user.cachedData.permissionData.checkPermission(permission)
    }

    fun getLoadedUser(uuid: UUID): User? = luckPerms.userManager.getUser(uuid)

    private companion object {
        val DATA_DIRECTORY = Path("plugins/luckperms")
    }
}
