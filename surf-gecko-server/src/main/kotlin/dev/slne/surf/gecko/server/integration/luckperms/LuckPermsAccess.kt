package dev.slne.surf.gecko.server.integration.luckperms

import dev.slne.surf.api.core.luckperms.LuckPermsAccess
import dev.slne.surf.api.core.luckperms.prefix
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.user.UserDataRecalculateEvent
import java.util.*

object LuckPermsAccess {
    fun prefix(playerUuid: UUID) =
        LuckPermsAccess.getUser(playerUuid)?.prefix

    fun weight(playerUuid: UUID) = LuckPermsAccess.getUser(playerUuid)?.primaryGroup?.let {
        LuckPermsAccess.luckperms.groupManager.getGroup(it)
    }?.weight?.orElse(0) ?: 0

    fun subscribeToRankChanges(
        onRankChange: () -> Unit
    ): EventSubscription<UserDataRecalculateEvent> = LuckPermsAccess.luckperms.eventBus.subscribe(
        UserDataRecalculateEvent::class.java
    ) { onRankChange() }
}
