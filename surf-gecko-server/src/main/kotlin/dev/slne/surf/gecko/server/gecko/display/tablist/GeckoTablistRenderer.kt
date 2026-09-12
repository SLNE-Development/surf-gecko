package dev.slne.surf.gecko.server.gecko.display.tablist

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.social.SocialGroup
import dev.slne.surf.gecko.server.gecko.social.SocialGroupManager
import dev.slne.surf.gecko.server.integration.luckperms.LuckPermsAccess
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GeckoTablistRenderer {
    private const val BOTTOM_ORDER = 1
    private const val HIDER_ORDER = 1_000
    private const val SEEKER_ORDER = 2_000
    private const val RANK_ORDER_BASE = 1_000

    private val UPDATE_ACTIONS = EnumSet.of(
        PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
        PlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
    )

    private val sent = ConcurrentHashMap<UUID, MutableMap<UUID, TabEntry>>()
    private val lock = Any()

    private var luckpermsListener: EventSubscription<UserDataRecalculateEvent>? = null

    fun init() {
        luckpermsListener = LuckPermsAccess.subscribeToRankChanges { refresh() }
    }

    fun shutdown() {
        luckpermsListener?.close()
        luckpermsListener = null
        sent.clear()
    }

    fun refresh(): Unit = synchronized(lock) {
        val players = MinecraftServer.getConnectionManager().onlinePlayers

        if (players.isEmpty()) {
            return
        }

        val profiles = players.map(::profileOf)

        for (viewer in profiles) {
            val cache = sent.getOrPut(viewer.player.uuid) { Object2ObjectOpenHashMap() }
            val changed = ArrayList<PlayerInfoUpdatePacket.Entry>()

            for (target in profiles) {
                val entry = entryFor(viewer, target)

                if (cache.put(target.player.uuid, entry) == entry) {
                    continue
                }

                changed += packetEntry(target.player, entry)
            }

            if (changed.isEmpty()) {
                continue
            }

            viewer.player.sendPacket(PlayerInfoUpdatePacket(UPDATE_ACTIONS, changed))
        }
    }

    fun reset(playerUuid: UUID) {
        sent.remove(playerUuid)
        sent.values.forEach { it.remove(playerUuid) }
    }

    private fun entryFor(viewer: TabProfile, target: TabProfile): TabEntry {
        if (!inSharedSection(viewer, target)) {
            return TabEntry(target.grayName, BOTTOM_ORDER)
        }

        return when (viewer.group) {
            SocialGroup.LOBBY, SocialGroup.GAME_ALL ->
                TabEntry(target.rankedName, target.rankOrder)

            else -> TabEntry(
                target.roleName ?: target.grayName,
                if (target.group == SocialGroup.GAME_SEEKER) SEEKER_ORDER else HIDER_ORDER
            )
        }
    }

    private fun inSharedSection(viewer: TabProfile, target: TabProfile) = when (viewer.group) {
        SocialGroup.LOBBY -> target.group == SocialGroup.LOBBY

        SocialGroup.GAME_ALL ->
            target.group == SocialGroup.GAME_ALL && viewer.gameId == target.gameId

        else -> viewer.gameId == target.gameId &&
                (target.group == SocialGroup.GAME_SEEKER || target.group == SocialGroup.GAME_HIDER)
    }

    private fun profileOf(player: Player): TabProfile {
        val game = GeckoGameManager.findGame(player.uuid)
        val role = game?.findGamePlayer(player.uuid)?.role
        val prefix = LuckPermsAccess.prefix(player.uuid)
        val weight = LuckPermsAccess.weight(player.uuid)

        return TabProfile(
            player = player,
            group = SocialGroupManager.groupOf(player.uuid),
            gameId = game?.internalId,
            rankedName = miniMessage.deserialize("$prefix${player.username}"),
            rankOrder = RANK_ORDER_BASE + weight,
            roleName = role?.let {
                buildText {
                    append(
                        BitmapProvider.translateToComponent(
                            it.displayName,
                            Colors.WHITE,
                            it.color,
                            affixAmount = 3
                        )
                    )
                    appendSpace()
                    text(player.username, it.color)
                }
            },
            grayName = miniMessage.deserialize("$prefix${player.username}").darkenColors()
        )
    }

    private fun packetEntry(player: Player, entry: TabEntry) = PlayerInfoUpdatePacket.Entry(
        player.uuid,
        player.username,
        emptyList(),
        true,
        player.latency,
        player.gameMode,
        entry.displayName,
        null,
        entry.listOrder,
        true
    )

    private fun Component.darkenColors(factor: Double = 0.45): Component {
        val color = style().color()
        val newColor = color?.let {
            TextColor.color(
                (it.red() * factor).toInt(),
                (it.green() * factor).toInt(),
                (it.blue() * factor).toInt()
            )
        }

        return children(
            children().map { it.darkenColors(factor) }
        ).style(
            style().color(newColor)
        )
    }
}
