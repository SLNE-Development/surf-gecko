// Credits to https://github.com/bradenk04/minestom-tab/ @ 08.09.2026 19:25 UTC+2

package dev.slne.surf.gecko.server.gecko.social.visibility

import dev.slne.surf.api.core.luckperms.LuckPermsAccess
import dev.slne.surf.api.core.luckperms.getLuckPermsUserOrNull
import dev.slne.surf.gecko.server.player.GeckoPlayer
import dev.slne.surf.gecko.server.player.requireGeckoPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.AddEntityToInstanceEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.network.packet.server.ServerPacket
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import net.minestom.server.timer.TaskSchedule
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class VisibilityManager(
    eventNode: EventNode<Event>,
    private val defaultGroup: VisibilityGroup = PerInstanceGroup
) {

    private val playerGroups = ConcurrentHashMap<Player, VisibilityGroup>()
    private val filtering = ThreadLocal.withInitial { false }

    init {
        eventNode.addListener(PlayerPacketOutEvent::class.java, ::handlePacketOut)
        eventNode.addListener(AddEntityToInstanceEvent::class.java, ::handleInstanceAdd)
        eventNode.addListener(PlayerSpawnEvent::class.java, ::handleSpawn)
        eventNode.addListener(PlayerDisconnectEvent::class.java, ::handleDisconnect)
    }

    fun setGroup(player: Player, group: VisibilityGroup) {
        playerGroups[player] = group
        refresh(player)
    }

    fun groupOf(player: Player) = playerGroups[player] ?: defaultGroup

    fun canSee(observer: Player, target: Player): Boolean {
        if (observer === target) return true

        return groupOf(observer).canSee(observer, target)
                && groupOf(target).canBeSeenBy(target, observer)
    }

    fun refresh(player: Player) = filtered {
        refreshForEveryone(player)

        player.updateViewableRule { observer -> canSee(observer, player) }
        player.updateViewerRule { entity -> entity !is Player || canSee(player, entity) }

        for (online in onlinePlayers()) {
            if (online === player) {
                refreshSpawnsFor(player)
                continue
            }

            if (canSee(online, player)) {
                online.sendPacket(createSpawnPacket(player))
            }
        }
    }

    private fun refreshSpawnsFor(observer: Player) {
        for (online in onlinePlayers()) {
            if (online === observer || !canSee(observer, online)) continue

            observer.sendPacket(createAddPacket(online))
            observer.sendPacket(createSpawnPacket(online))
        }
    }

    private fun refreshForEveryone(subject: Player) {
        for (online in onlinePlayers()) {
            if (online === subject) {
                refreshViewOfOthers(subject)
                continue
            }

            if (canSee(online, subject)) {
                online.sendPacket(createAddPacket(subject))
            } else {
                online.sendPacket(PlayerInfoRemovePacket(subject.uuid))
            }
        }
    }

    private fun refreshViewOfOthers(observer: Player) {
        val entries = mutableListOf<PlayerInfoUpdatePacket.Entry>()
        val toRemove = mutableListOf<UUID>()

        for (online in onlinePlayers()) {
            if (online === observer) continue

            if (canSee(observer, online)) {
                entries += createEntry(online.requireGeckoPlayer())
            } else {
                toRemove += online.uuid
            }
        }

        if (toRemove.isNotEmpty()) {
            observer.sendPacket(PlayerInfoRemovePacket(toRemove))
        }

        if (entries.isNotEmpty()) {
            observer.sendPacket(PlayerInfoUpdatePacket(ALL_ACTIONS, entries))
        }
    }

    private fun handlePacketOut(event: PlayerPacketOutEvent) {
        if (filtering.get()) return

        when (val packet = event.packet) {
            is PlayerInfoUpdatePacket -> handleUpdatePacket(event, packet)
            is SpawnEntityPacket -> handleSpawnPacket(event, packet)
            else -> Unit
        }
    }

    private fun handleSpawnPacket(event: PlayerPacketOutEvent, packet: SpawnEntityPacket) {
        if (packet.type() != EntityType.PLAYER) return

        val target = onlinePlayer(packet.uuid()) ?: return

        if (!canSee(event.player, target)) {
            event.isCancelled = true
        }
    }

    private fun handleUpdatePacket(event: PlayerPacketOutEvent, packet: PlayerInfoUpdatePacket) {
        val observer = event.player
        val entries = packet.entries()
        val filteredEntries = entries.filter { entry ->
            val target = onlinePlayer(entry.uuid())

            target == null || canSee(observer, target)
        }

        if (filteredEntries.size == entries.size) return

        event.isCancelled = true

        if (filteredEntries.isNotEmpty()) {
            resendPacket(observer, PlayerInfoUpdatePacket(packet.actions(), filteredEntries))
        }
    }

    private fun resendPacket(player: Player, packet: ServerPacket) = filtered {
        player.sendPacket(packet)
    }

    private fun handleInstanceAdd(event: AddEntityToInstanceEvent) {
        val player = event.entity as? Player ?: return

        refreshNextTick(player)
    }

    private fun handleSpawn(event: PlayerSpawnEvent) = refreshNextTick(event.player)

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        playerGroups.remove(event.player)
    }

    private fun refreshNextTick(player: Player) {
        MinecraftServer.getSchedulerManager()
            .buildTask { refresh(player) }
            .delay(TaskSchedule.nextTick())
            .schedule()
    }

    private fun createAddPacket(player: Player) =
        PlayerInfoUpdatePacket(ALL_ACTIONS, listOf(createEntry(player.requireGeckoPlayer())))

    private fun createEntry(player: GeckoPlayer): PlayerInfoUpdatePacket.Entry {
        val skin = player.skin
        val properties = if (skin == null) {
            emptyList()
        } else {
            listOf(PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()))
        }

        return PlayerInfoUpdatePacket.Entry(
            player.uuid,
            player.username,
            properties,
            true,
            player.latency,
            player.gameMode,
            player.displayName,
            player.chatSession(),
            player.getLuckPermsUserOrNull()?.primaryGroup?.let {
                LuckPermsAccess.luckperms.groupManager.getGroup(
                    it
                )?.weight?.asInt ?: 0
            } ?: 0,
            true
        )
    }

    private fun createSpawnPacket(player: Player) = SpawnEntityPacket(
        player.entityId,
        player.uuid,
        EntityType.PLAYER,
        player.position,
        player.position.yaw(),
        0,
        Vec.ZERO
    )

    private fun onlinePlayers() = MinecraftServer.getConnectionManager().onlinePlayers

    private fun onlinePlayer(uuid: UUID) =
        MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)

    private inline fun <T> filtered(block: () -> T): T {
        filtering.set(true)

        try {
            return block()
        } finally {
            filtering.set(false)
        }
    }

    companion object {
        private val ALL_ACTIONS: EnumSet<PlayerInfoUpdatePacket.Action> =
            EnumSet.allOf(PlayerInfoUpdatePacket.Action::class.java)
    }
}
