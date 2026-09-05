@file:OptIn(NmsUseWithCaution::class)
@file:Suppress("UnstableApiUsage")

package dev.slne.surf.gecko.map.creator.render

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.gecko.map.creator.PaperGeckoMapManager
import dev.slne.surf.gecko.map.creator.draft.DraftPos
import dev.slne.surf.gecko.map.creator.draft.GeckoPoiType
import dev.slne.surf.gecko.map.creator.plugin
import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

object MarkerRenderer {
    private const val RENDER_DISTANCE = 96.0
    private const val LABEL_DISTANCE = 28.0
    private const val MAX_MARKERS_PER_PLAYER = 128
    private const val MAX_LABELS_PER_PLAYER = 16

    private const val NO_ENTITY = -1
    private const val PILLAR_WIDTH = 0.3f
    private const val PILLAR_HEIGHT = 2.6f
    private const val ORB_SIZE = 0.45f
    private const val ORB_FLOAT_HEIGHT = 0.35f
    private const val ARROW_OFFSET = 1.4
    private const val ARROW_HEIGHT = 1.0
    private const val ARROW_SIZE = 0.22f
    private const val LABEL_OFFSET = 0.45f
    private const val LABEL_SCALE = 0.9f

    private val TICK_INTERVAL = 400.milliseconds
    private val arrowBlockData by lazy { Material.WHITE_CONCRETE.createBlockData() }

    private val entityIds = AtomicInteger(Int.MAX_VALUE)
    private val views = ConcurrentHashMap<UUID, PlayerView>()

    private var task: Job? = null

    fun start() {
        task = plugin.scope.runAtFixedRate(TICK_INTERVAL, taskName = "gecko-marker-renderer") {
            for (playerUuid in views.keys) {
                val player = Bukkit.getPlayer(playerUuid)
                if (player == null) {
                    views.remove(playerUuid)
                    continue
                }

                withContext(plugin.entityDispatcher(player)) { render(player) }
            }
        }
    }

    fun stop() {
        task?.cancel()
        task = null

        for (playerUuid in views.keys.toList()) {
            val view = views.remove(playerUuid) ?: continue
            val player = Bukkit.getPlayer(playerUuid) ?: continue
            despawnAll(player, view)
        }
    }

    fun enable(player: Player) {
        views.putIfAbsent(player.uniqueId, PlayerView())
    }

    fun disable(player: Player) {
        val view = views.remove(player.uniqueId) ?: return
        despawnAll(player, view)
    }

    fun isEnabled(player: Player) = views.containsKey(player.uniqueId)

    fun toggle(player: Player): Boolean {
        if (isEnabled(player)) {
            disable(player)
            return false
        }

        enable(player)
        return true
    }

    fun forget(player: Player) {
        views.remove(player.uniqueId)
    }

    fun refresh(player: Player) {
        if (isEnabled(player)) render(player)
    }

    private fun render(player: Player) {
        val view = views[player.uniqueId] ?: return
        val draft = PaperGeckoMapManager.draft(player.uniqueId)

        if (draft == null || player.world.name != draft.worldName) {
            despawnAll(player, view)
            return
        }

        val desired = collectDesired(draft.locationsByType(), player)
        val despawnIds = IntArrayList()
        val operations = ArrayList<PacketOperation>()

        val iterator = view.rendered.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val rendered = entry.value
            val target = desired[entry.key]

            if (target != null &&
                target.pos == rendered.pos &&
                target.withLabel == (rendered.labelId != NO_ENTITY)
            ) {
                continue
            }

            rendered.collectIds(despawnIds)
            iterator.remove()
        }

        for ((key, target) in desired) {
            if (view.rendered.containsKey(key)) continue
            view.rendered[key] = spawn(operations, key, target)
        }

        if (!despawnIds.isEmpty) {
            operations.add(SurfPaperNmsSpawnPackets.despawn(despawnIds))
        }

        execute(operations, player)
    }

    private fun execute(operations: List<PacketOperation>, player: Player) {
        if (operations.isEmpty()) return

        var combined = operations.first()
        for (index in 1 until operations.size) {
            combined = combined.add(operations[index])
        }

        if (!combined.isEmpty()) {
            combined.execute(player)
        }
    }

    private fun collectDesired(
        locationsByType: Map<GeckoPoiType, List<DraftPos>>,
        player: Player,
    ): Map<MarkerKey, Desired> {
        val eyeLocation = player.eyeLocation
        val renderDistanceSquared = RENDER_DISTANCE * RENDER_DISTANCE
        val labelDistanceSquared = LABEL_DISTANCE * LABEL_DISTANCE

        val candidates = ArrayList<Candidate>()
        for ((type, locations) in locationsByType) {
            for (index in locations.indices) {
                val pos = locations[index]
                val distanceSquared = pos.distanceSquaredTo(eyeLocation)
                if (distanceSquared > renderDistanceSquared) continue

                candidates.add(Candidate(MarkerKey(type, index), pos, distanceSquared))
            }
        }

        candidates.sortBy { it.distanceSquared }

        val desired = HashMap<MarkerKey, Desired>()
        var labelBudget = MAX_LABELS_PER_PLAYER

        for (candidate in candidates) {
            if (desired.size >= MAX_MARKERS_PER_PLAYER) break

            val withLabel = labelBudget > 0 && candidate.distanceSquared <= labelDistanceSquared
            if (withLabel) labelBudget--

            desired[candidate.key] = Desired(candidate.pos, withLabel)
        }

        return desired
    }

    private fun spawn(
        operations: MutableList<PacketOperation>,
        key: MarkerKey,
        target: Desired,
    ): Rendered {
        val type = key.type
        val pos = target.pos
        val bodyId = nextEntityId()
        val bodyHeight = if (type.multiple) ORB_SIZE + ORB_FLOAT_HEIGHT else PILLAR_HEIGHT

        operations.add(
            SurfPaperNmsSpawnPackets.spawnBlockDisplay(bodyId, Position.fine(pos.x, pos.y, pos.z)) {
                blockData = type.markerBlockData
                if (type.multiple) {
                    scale = Vector3f(ORB_SIZE, ORB_SIZE, ORB_SIZE)
                    translation = Vector3f(-ORB_SIZE / 2f, ORB_FLOAT_HEIGHT, -ORB_SIZE / 2f)
                } else {
                    scale = Vector3f(PILLAR_WIDTH, PILLAR_HEIGHT, PILLAR_WIDTH)
                    translation = Vector3f(-PILLAR_WIDTH / 2f, 0f, -PILLAR_WIDTH / 2f)
                }
            }
        )

        var arrowId = NO_ENTITY
        if (type.withRotation) {
            arrowId = nextEntityId()

            val yawRadians = Math.toRadians(pos.yaw.toDouble())
            val arrowX = pos.x - sin(yawRadians) * ARROW_OFFSET
            val arrowZ = pos.z + cos(yawRadians) * ARROW_OFFSET

            operations.add(
                SurfPaperNmsSpawnPackets.spawnBlockDisplay(
                    arrowId,
                    Position.fine(arrowX, pos.y + ARROW_HEIGHT, arrowZ)
                ) {
                    blockData = arrowBlockData
                    scale = Vector3f(ARROW_SIZE, ARROW_SIZE, ARROW_SIZE)
                    translation = Vector3f(-ARROW_SIZE / 2f, -ARROW_SIZE / 2f, -ARROW_SIZE / 2f)
                }
            )
        }

        var labelId = NO_ENTITY
        if (target.withLabel) {
            labelId = nextEntityId()

            val label = buildText {
                text(type.displayName, type.color, TextDecoration.BOLD)
                if (type.multiple) {
                    text(" #" + (key.index + 1), NamedTextColor.WHITE)
                }
            }

            operations.add(
                SurfPaperNmsSpawnPackets.spawnTextDisplay(
                    labelId,
                    Position.fine(pos.x, pos.y + bodyHeight + LABEL_OFFSET, pos.z)
                ) {
                    text = label
                    billboardConstraints = Display.Billboard.CENTER
                    scale = Vector3f(LABEL_SCALE, LABEL_SCALE, LABEL_SCALE)
                }
            )
        }

        return Rendered(pos, bodyId, arrowId, labelId)
    }

    private fun despawnAll(player: Player, view: PlayerView) {
        if (view.rendered.isEmpty()) return

        val ids = IntArrayList()
        for (rendered in view.rendered.values) {
            rendered.collectIds(ids)
        }
        view.rendered.clear()

        SurfPaperNmsSpawnPackets.despawn(ids).execute(player)
    }

    private fun nextEntityId() = entityIds.getAndDecrement()

    private data class MarkerKey(val type: GeckoPoiType, val index: Int)

    private class Candidate(val key: MarkerKey, val pos: DraftPos, val distanceSquared: Double)

    private class Desired(val pos: DraftPos, val withLabel: Boolean)

    private class Rendered(
        val pos: DraftPos,
        val bodyId: Int,
        val arrowId: Int,
        val labelId: Int,
    ) {
        fun collectIds(target: IntArrayList) {
            target.add(bodyId)
            if (arrowId != NO_ENTITY) target.add(arrowId)
            if (labelId != NO_ENTITY) target.add(labelId)
        }
    }

    private class PlayerView {
        val rendered = HashMap<MarkerKey, Rendered>()
    }
}
