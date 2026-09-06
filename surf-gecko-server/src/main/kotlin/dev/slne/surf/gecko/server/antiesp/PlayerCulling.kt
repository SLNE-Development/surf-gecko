package dev.slne.surf.gecko.server.antiesp

import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.coroutine.geckoScope
import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.potion.PotionEffect
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private val NOT_HIDDEN = IntArray(0)
private val INTERVAL = 100.milliseconds

object PlayerCulling {
    private val states = ConcurrentHashMap<Player, PlayerCullState>()
    private var job: Job? = null

    fun init() {
        if (job != null) {
            return
        }

        job = geckoAsyncScope.runAtFixedRate(INTERVAL, taskName = "player-culling") { cull() }
    }

    fun shutdown() {
        job?.cancel()
        job = null

        states.keys.forEach { it.updateViewableRule(null) }
        states.clear()
    }

    private suspend fun cull() {
        val online = MinecraftServer.getConnectionManager().onlinePlayers

        states.keys.removeIf { !it.isOnline }

        val byInstance = mutableMapOf<Instance, MutableList<Player>>()

        online.forEach { player ->
            val instance = player.instance ?: return@forEach
            byInstance.getOrPut(instance) { mutableListOf() }.add(player)
        }

        if (byInstance.isEmpty()) {
            return
        }

        val outdated = coroutineScope {
            byInstance.map { (instance, players) -> async { cullInstance(instance, players) } }
                .awaitAll()
        }.flatten()

        if (outdated.isEmpty()) {
            return
        }

        geckoScope.launch { outdated.forEach(PlayerCullState::apply) }
    }

    private fun cullInstance(
        instance: Instance,
        players: MutableList<Player>
    ): List<PlayerCullState> {
        val size = players.size

        if (size < 2) {
            return players.mapNotNull { player ->
                states[player]?.takeIf { it.update(NOT_HIDDEN) }
            }
        }

        players.sortBy { it.entityId }

        val hidden = arrayOfNulls<IntArrayList>(size)
        val alwaysVisible = BooleanArray(size) { players[it].isAlwaysVisible() }
        val seesThroughWalls = BooleanArray(size) { players[it].seesThroughWalls() }

        OcclusionRaycaster(instance).use { raycaster ->
            for (viewerIndex in 0 until size) {
                val viewer = players[viewerIndex]

                for (targetIndex in viewerIndex + 1 until size) {
                    val target = players[targetIndex]

                    val hideViewer =
                        !alwaysVisible[viewerIndex] && !seesThroughWalls[targetIndex]
                    val hideTarget =
                        !alwaysVisible[targetIndex] && !seesThroughWalls[viewerIndex]

                    if (!hideViewer && !hideTarget) {
                        continue
                    }

                    if (raycaster.hasLineOfSight(viewer, target)) {
                        continue
                    }

                    if (hideViewer) hidden.hide(viewerIndex, target.entityId)
                    if (hideTarget) hidden.hide(targetIndex, viewer.entityId)
                }
            }
        }

        val outdated = mutableListOf<PlayerCullState>()

        for (index in 0 until size) {
            val player = players[index]
            val hiddenIds = hidden[index]?.toIntArray() ?: NOT_HIDDEN
            val known = states[player]

            if (known == null) {
                val state = PlayerCullState(player)
                state.update(hiddenIds)
                states[player] = state
                outdated.add(state)
            } else if (known.update(hiddenIds)) {
                outdated.add(known)
            }
        }

        return outdated
    }

    private fun Array<IntArrayList?>.hide(index: Int, entityId: Int) {
        val list = this[index] ?: IntArrayList().also { this[index] = it }
        list.add(entityId)
    }
}

private fun Player.isAlwaysVisible() = isGlowing || hasEffect(PotionEffect.GLOWING)

private fun Player.seesThroughWalls() =
    gameMode == GameMode.SPECTATOR || gameMode == GameMode.CREATIVE

private class PlayerCullState(private val player: Player) {
    @Volatile
    private var hidden = NOT_HIDDEN
    private var installed = false

    fun update(hiddenIds: IntArray): Boolean {
        if (hiddenIds.contentEquals(hidden)) {
            return false
        }

        hidden = hiddenIds
        return true
    }

    fun apply() {
        if (installed) {
            player.updateViewableRule()
            return
        }

        installed = true
        player.updateViewableRule { viewer -> !conceals(viewer.entityId) }
    }

    private fun conceals(entityId: Int): Boolean {
        val current = hidden

        for (hiddenId in current) {
            if (hiddenId == entityId) return true
        }

        return false
    }
}
