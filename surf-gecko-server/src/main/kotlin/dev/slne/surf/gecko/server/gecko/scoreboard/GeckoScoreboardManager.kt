package dev.slne.surf.gecko.server.gecko.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.bitmap.common.provider.BitmapProvider
import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import dev.slne.surf.gecko.server.gecko.state.GeckoGameState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.scoreboard.Sidebar
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

object GeckoScoreboardManager {
    private val sidebars = ConcurrentHashMap<ULong, Sidebar>()

    fun createSidebar(game: GeckoGame) {
        val sidebar = Sidebar(buildText {
            note("      Hide 'n Seek      ", TextDecoration.BOLD)
        })

        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "1",
                Component.empty(),
                9,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "2",
                Component.empty(),
                8,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "3",
                Component.empty(),
                7,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "4",
                Component.empty(),
                6,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "5",
                Component.empty(),
                5,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "6",
                Component.empty(),
                4,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "7",
                Component.empty(),
                3,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(
            Sidebar.ScoreboardLine(
                "8",
                Component.empty(),
                2,
                Sidebar.NumberFormat.blank()
            )
        )
        sidebar.createLine(Sidebar.ScoreboardLine("9", buildText {
            note("castcrafter.de".toSmallCaps())
        }, 1, Sidebar.NumberFormat.blank()))

        sidebars[game.internalId] = sidebar
    }

    fun updateSidebar(game: GeckoGame) {
        val sidebar = sidebars[game.internalId] ?: return

        sidebar.updateLineContent(
            "2",
            BitmapProvider.translateToComponent("Map", Colors.WHITE, Colors.INFO)
        )
        sidebar.updateLineContent("3", buildText {
            white(game.settings.map.mapDisplayName)
        })

        when (game.state) {
            GeckoGameState.LOBBY -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent("5", buildText {
                    append(
                        BitmapProvider.translateToComponent(
                            "Spieler",
                            Colors.WHITE,
                            Colors.INFO
                        )
                    )
                })
                sidebar.updateLineContent("6", buildText {
                    white(
                        "${
                            game.lobbyPlayers.size.toString().padStart(2, '0')
                        }/${game.settings.maxPlayers.toString().padStart(2, '0')} 👥"
                    )
                })

                sidebar.updateLineContent("7", Component.empty())
                sidebar.updateLineContent(
                    "8",
                    buildText {
                        append(
                            BitmapProvider.translateToComponent(
                                "Wartezeit:",
                                Colors.WHITE,
                                Colors.INFO
                            )
                        )
                        appendSpace()
                        if (game.countdownSeconds == null) {
                            white("Warten...")
                        } else {
                            append(getGameTime(game).color(Colors.WHITE))
                            white(" ⌚")
                        }
                    }
                )
            }

            GeckoGameState.HIDING, GeckoGameState.SEARCHING -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent("5", buildText {
                    append(
                        BitmapProvider.translateToComponent(
                            game.gamePlayers.count { it.role == GeckoGameRole.SEEKER }.toString()
                                .padStart(2, '0'),
                            Colors.WHITE,
                            GeckoGameRole.SEEKER.color,
                            affixAmount = 3
                        )
                    )
                    spacer(" / ")
                    append(
                        BitmapProvider.translateToComponent(
                            game.gamePlayers.count { it.role == GeckoGameRole.HIDER }.toString()
                                .padStart(2, '0'),
                            Colors.WHITE,
                            GeckoGameRole.HIDER.color,
                            affixAmount = 3
                        )
                    )
                    white(" 👥")
                })
                sidebar.updateLineContent("6", Component.empty())

                sidebar.updateLineContent("7", buildText {
                    append(BitmapProvider.translateToComponent("Zeit: ", Colors.WHITE, Colors.INFO))
                    append(getGameTime(game).color(Colors.WHITE))
                    white(" ⌚")
                })
                sidebar.updateLineContent("8", Component.empty())
            }

            GeckoGameState.ENDING, GeckoGameState.ENDED -> {
                sidebar.updateLineContent("4", Component.empty())
                sidebar.updateLineContent("5", Component.empty())
                sidebar.updateLineContent(
                    "6",
                    BitmapProvider.translateToComponent("Spielende", Colors.WHITE, Colors.INFO)
                )
                sidebar.updateLineContent("7", buildText {
                    white(
                        formatSeconds(
                            game.endingTimerSeconds ?: Duration.ZERO.inWholeSeconds.toInt()
                        )
                    )
                    white(" ⌚")
                })
                sidebar.updateLineContent("8", Component.empty())
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