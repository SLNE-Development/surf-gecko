package dev.slne.surf.gecko.server.gecko.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.scoreboard.Sidebar
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object GeckoScoreboardManager {
    private val sidebars = ConcurrentHashMap<ULong, Sidebar>()

    fun createSidebar(game: GeckoGame) {
        val sidebar = Sidebar(buildText {
            note("Hide 'n Seek")
        })

        sidebar.createLine(Sidebar.ScoreboardLine("1", Component.empty(), 1))
        sidebar.createLine(Sidebar.ScoreboardLine("2", Component.empty(), 2))
        sidebar.createLine(Sidebar.ScoreboardLine("3", Component.empty(), 3))
        sidebar.createLine(Sidebar.ScoreboardLine("4", Component.empty(), 4))
        sidebar.createLine(Sidebar.ScoreboardLine("5", Component.empty(), 5))
        sidebar.createLine(Sidebar.ScoreboardLine("6", Component.empty(), 6))
        sidebar.createLine(Sidebar.ScoreboardLine("7", Component.empty(), 7))
        sidebar.createLine(Sidebar.ScoreboardLine("8", Component.empty(), 8))
        sidebar.createLine(Sidebar.ScoreboardLine("9", buildText {
            primary("castcrafter.de".toSmallCaps())
        }, 9))

        sidebars[game.internalId] = sidebar
    }

    fun updateSidebar(game: GeckoGame) {
        val sidebar = sidebars[game.internalId] ?: return

        sidebar.updateLineContent("2", buildText {
            variableValue("Map")
            spacer(": ")
            white(game.settings.map.mapDisplayName)
        })

        when (game.state) {
            GeckoGameState.LOBBY -> {
                sidebar.updateLineContent("4", buildText {
                    variableValue("Spieler")
                    white(": ${game.lobbyPlayers.size.toString().padStart(2, '0')} 👥")
                })

                sidebar.updateLineContent("6", buildText {
                    variableValue("Zeit: ")
                    append(getGameTime(game).color(Colors.WHITE))
                })
            }

            GeckoGameState.HIDING, GeckoGameState.SEARCHING -> {
                sidebar.updateLineContent("4", buildText {
                    text("Sucher", GeckoGameRole.SEEKER.color)
                    spacer(": ")
                    white(
                        game.gamePlayers.count { it.role == GeckoGameRole.SEEKER }.toString()
                            .padStart(2, '0')
                    )
                    white(" 👥")
                })

                sidebar.updateLineContent("5", buildText {
                    text("Verstecker", GeckoGameRole.HIDER.color)
                    spacer(": ")
                    white(
                        game.gamePlayers.count { it.role == GeckoGameRole.HIDER }.toString()
                            .padStart(2, '0')
                    )
                    white(" 👥")
                })

                sidebar.updateLineContent("6", Component.empty())

                sidebar.updateLineContent("7", buildText {
                    variableValue("Zeit: ")
                    append(getGameTime(game).color(Colors.WHITE))
                })
            }

            GeckoGameState.ENDING, GeckoGameState.ENDED -> {
                sidebar.updateLineContent("7", buildText {
                    variableValue("Zeit: ")
                    append(
                        CommonComponents.formatTime(
                            game.endingTimerSeconds?.seconds ?: Duration.ZERO,
                            showSeconds = true,
                            shortForms = false
                        ).color(Colors.WHITE)
                    )
                })
            }

            else -> {

            }
        }
    }

    fun showSidebar(game: GeckoGame, player: Player) {
        val sidebar = sidebars[game.internalId] ?: return

        sidebars.values.forEach {
            if (it !== sidebar) it.removeViewer(player)
        }

        updateSidebar(game)
        sidebar.addViewer(player)
    }

    fun hideSidebar(player: Player) {
        sidebars.values.forEach { it.removeViewer(player) }
    }

    fun getSidebar(game: GeckoGame) = sidebars[game.internalId]

    fun removeSidebar(game: GeckoGame) {
        val sidebar = sidebars.remove(game.internalId) ?: return
        sidebar.viewers.toSet().forEach { sidebar.removeViewer(it) }
    }

    fun getGameTime(game: GeckoGame) = if (game.state.isGame()) {
        CommonComponents.formatTime(
            game.gameTimerSeconds?.seconds ?: Duration.ZERO,
            showSeconds = true,
            shortForms = false
        )
    } else {
        CommonComponents.formatTime(
            game.countdownSeconds?.seconds ?: Duration.ZERO,
            showSeconds = true,
            shortForms = false
        )
    }
}