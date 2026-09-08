package dev.slne.surf.gecko.server.gecko.lobby.leaderbord

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.MinestomDispatchers
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.database.repository.GeckoLeaderboardRepository
import dev.slne.surf.gecko.server.database.repository.GeckoPlayerNameRepository
import dev.slne.surf.gecko.server.event.register
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.util.geckoHighlight
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import dev.slne.surf.gecko.server.gecko.util.geckoUseless
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object LeaderboardManager {
    private const val TOP_SIZE = 10
    private val minRefreshAge = 3.seconds

    private val snapshots = ConcurrentHashMap<LeaderboardType, LeaderboardSnapshot>()
    private val holograms = ConcurrentHashMap<UUID, Map<LeaderboardType, LeaderboardHologram>>()
    private val refreshMutex = Mutex()

    @Volatile
    private var lastRefresh = 0L

    private var refreshJob: Job? = null

    suspend fun init() {
        LeaderboardListener.register()

        refreshJob = geckoAsyncScope.runAtFixedRate(5.minutes) {
            refresh()
            updateAll()
        }
    }

    fun shutdown() {
        refreshJob?.cancel()
        refreshJob = null

        holograms.keys.toList().forEach { invalidate(it) }
    }

    suspend fun show(player: Player) {
        if (!GeckoLobby.contains(player)) {
            return
        }

        refreshIfDirty()

        withContext(MinestomDispatchers.Main) {
            if (!player.isOnline || !GeckoLobby.contains(player)) {
                return@withContext
            }

            val playerHolograms = holograms.computeIfAbsent(player.uuid) { playerUuid ->
                LeaderboardType.entries.associateWith { type ->
                    LeaderboardHologram(playerUuid).also {
                        it.spawn(GeckoLobby.instance, type.position)
                    }
                }
            }

            update(player, playerHolograms)
        }
    }

    fun invalidate(playerUuid: UUID) {
        val playerHolograms = holograms.remove(playerUuid) ?: return

        geckoScope.launch {
            playerHolograms.values.forEach { it.remove() }
        }
    }

    private suspend fun refresh() = refreshMutex.withLock { collectSnapshots() }

    private suspend fun refreshIfDirty() {
        refreshMutex.withLock {
            if (checkDirty()) {
                collectSnapshots()
            }
        }
    }

    private fun checkDirty() =
        System.currentTimeMillis() - lastRefresh >= minRefreshAge.inWholeMilliseconds

    private suspend fun collectSnapshots() {
        val totals = mapOf(
            LeaderboardType.WINS to GeckoLeaderboardRepository.fetchWinTotals(),
            LeaderboardType.KILLS to GeckoLeaderboardRepository.fetchKillTotals()
        )

        val topUuids = totals.values
            .flatMap { entries -> entries.take(TOP_SIZE) }
            .mapTo(mutableSetOf()) { it.playerUuid }

        val names = GeckoPlayerNameRepository.fetchNames(topUuids)

        totals.forEach { (type, entries) ->
            snapshots[type] = LeaderboardSnapshot.of(entries, names, TOP_SIZE)
        }

        lastRefresh = System.currentTimeMillis()
    }

    private suspend fun updateAll() = withContext(MinestomDispatchers.Main) {
        holograms.forEach { (playerUuid, playerHolograms) ->
            val player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerUuid)

            if (player == null) {
                invalidate(playerUuid)
            } else {
                update(player, playerHolograms)
            }
        }
    }

    private fun update(
        player: Player,
        playerHolograms: Map<LeaderboardType, LeaderboardHologram>
    ) = playerHolograms.forEach { (type, hologram) -> hologram.update(render(type, player)) }

    private fun render(type: LeaderboardType, player: Player): Component {
        val snapshot = snapshots[type] ?: LeaderboardSnapshot.EMPTY
        val place = snapshot.place(player.uuid)

        return buildText {
            geckoHighlight(type.title.toSmallCaps(), TextDecoration.BOLD)
            appendNewline()

            if (snapshot.top.isEmpty()) {
                geckoUseless("Noch keine Daten")
            } else {
                snapshot.top.forEach { entry ->
                    appendNewline()
                    appendRow(entry.rank.toString(), entry.name, entry.value)
                }
            }

            appendNewline()
            geckoUseless("▬▬▬▬▬▬▬▬▬")
            appendNewline()
            appendRow(place?.rank?.toString() ?: "-", "Du", place?.value ?: 0L)
        }
    }

    private fun SurfComponentBuilder.appendRow(rank: String, name: String, value: Long) {
        geckoPrimary("#$rank")
        spacer(" - ")
        geckoSecondary(name)
        spacer(" - ")
        geckoHighlight(value.toString())
    }
}
