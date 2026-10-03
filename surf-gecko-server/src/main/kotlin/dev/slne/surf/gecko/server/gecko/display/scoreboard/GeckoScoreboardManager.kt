package dev.slne.surf.gecko.server.gecko.display.scoreboard

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.PlayerLanguages
import dev.slne.surf.gecko.server.i18n.language
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.scoreboard.Sidebar
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

object GeckoScoreboardManager {
    private val sidebars = ConcurrentHashMap<ULong, ConcurrentHashMap<GeckoLanguage, Sidebar>>()

    init {
        PlayerLanguages.onChange { player ->
            val game = GeckoGameManager.findGame(player.uuid) ?: return@onChange
            val gameSidebars = sidebars[game.internalId] ?: return@onChange

            if (gameSidebars.values.any { it.isViewer(player) }) {
                showSidebar(game, player)
            }
        }
    }

    fun createSidebar(game: GeckoGame) {
        sidebars[game.internalId] = ConcurrentHashMap()
    }

    private fun sidebar(game: GeckoGame, language: GeckoLanguage) =
        sidebars[game.internalId]?.computeIfAbsent(language, ::newSidebar)

    private fun newSidebar(language: GeckoLanguage): Sidebar {
        val sidebar = Sidebar(GeckoTranslations.render(language, "display.scoreboard.title"))

        for (line in 1..8) {
            sidebar.createLine(
                Sidebar.ScoreboardLine(
                    line.toString(),
                    Component.empty(),
                    10 - line,
                    Sidebar.NumberFormat.blank()
                )
            )
        }
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "9",
                GeckoTranslations.render(language, "display.scoreboard.footer"),
                1,
                Sidebar.NumberFormat.blank()
            )
        )

        return sidebar
    }

    fun updateSidebar(game: GeckoGame) {
        sidebars[game.internalId]?.forEach { (language, sidebar) -> updateSidebar(game, language, sidebar) }
    }

    private fun updateSidebar(game: GeckoGame, language: GeckoLanguage, sidebar: Sidebar) {
        sidebar.updateLineContent("2", heading(language, "display.scoreboard.heading.map"))
        sidebar.updateLineContent(
            "3",
            GeckoTranslations.render(language, "display.scoreboard.map", "map" to game.settings.map.displayName)
        )

        when (game.state) {
            GeckoGameState.LOBBY -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent("5", heading(language, "display.scoreboard.heading.players"))
                sidebar.updateLineContent(
                    "6",
                    GeckoTranslations.render(
                        language,
                        "display.scoreboard.players",
                        "players" to game.lobbyPlayers.size.toString().padStart(2, '0'),
                        "max" to game.settings.maxPlayers.toString().padStart(2, '0')
                    )
                )

                sidebar.updateLineContent("7", Component.empty())
                sidebar.updateLineContent(
                    "8",
                    Component.textOfChildren(
                        heading(language, "display.scoreboard.heading.waiting-time"),
                        Component.space(),
                        if (game.countdownSeconds == null) {
                            GeckoTranslations.render(language, "display.scoreboard.waiting")
                        } else {
                            timeText(language, getGameTime(game))
                        }
                    )
                )
            }

            GeckoGameState.HIDING, GeckoGameState.SEARCHING -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent(
                    "5",
                    GeckoTranslations.render(
                        language,
                        "display.scoreboard.roles",
                        "seekers" to roleCount(game, GeckoGameRole.SEEKER),
                        "hiders" to roleCount(game, GeckoGameRole.HIDER)
                    )
                )
                sidebar.updateLineContent("6", Component.empty())

                sidebar.updateLineContent(
                    "7",
                    Component.textOfChildren(
                        heading(language, "display.scoreboard.heading.time"),
                        timeText(language, getGameTime(game))
                    )
                )
                sidebar.updateLineContent("8", Component.empty())
            }

            GeckoGameState.ENDING, GeckoGameState.ENDED -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent("5", heading(language, "display.scoreboard.heading.ending"))
                sidebar.updateLineContent(
                    "6",
                    timeText(
                        language,
                        Component.text(
                            formatSeconds(game.endingTimerSeconds ?: Duration.ZERO.inWholeSeconds.toInt())
                        )
                    )
                )

                sidebar.updateLineContent("7", Component.empty())
                sidebar.updateLineContent("8", Component.empty())
            }

            else -> {

            }
        }
    }

    private fun heading(language: GeckoLanguage, key: String) =
        BitmapProvider.translateToComponent(
            GeckoTranslations.renderPlain(language, key),
            Colors.WHITE,
            Colors.INFO
        )

    private fun timeText(language: GeckoLanguage, time: Component) =
        GeckoTranslations.render(language, "display.scoreboard.time", "time" to time)

    private fun roleCount(game: GeckoGame, role: GeckoGameRole) = BitmapProvider.translateToComponent(
        game.gamePlayers.count { it.role == role }.toString().padStart(2, '0'),
        Colors.WHITE,
        role.color,
        affixAmount = 3
    )

    fun showSidebar(game: GeckoGame, player: Player) {
        val language = player.language
        val sidebar = sidebar(game, language) ?: return

        sidebars.values.forEach { gameSidebars ->
            gameSidebars.values.forEach {
                if (it !== sidebar) it.removeViewer(player)
            }
        }

        updateSidebar(game, language, sidebar)
        sidebar.addViewer(player)
    }

    fun hideSidebar(player: Player) {
        sidebars.values.forEach { gameSidebars -> gameSidebars.values.forEach { it.removeViewer(player) } }
    }

    fun getSidebar(game: GeckoGame, language: GeckoLanguage) = sidebars[game.internalId]?.get(language)

    fun removeSidebar(game: GeckoGame) {
        val gameSidebars = sidebars.remove(game.internalId) ?: return
        gameSidebars.values.forEach { sidebar ->
            sidebar.viewers.toSet().forEach { sidebar.removeViewer(it) }
        }
    }

    fun getGameTime(game: GeckoGame) = if (game.state.isGame()) {
        Component.text(formatSeconds(game.gameTimerSeconds ?: Duration.ZERO.inWholeSeconds.toInt()))
    } else {
        Component.text(formatSeconds(game.countdownSeconds ?: Duration.ZERO.inWholeSeconds.toInt()))
    }

    private fun formatSeconds(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}
