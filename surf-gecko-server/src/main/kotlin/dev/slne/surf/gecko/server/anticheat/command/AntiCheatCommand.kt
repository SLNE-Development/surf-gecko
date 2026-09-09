package dev.slne.surf.gecko.server.anticheat.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.gecko.server.anticheat.AntiCheatTracker
import dev.slne.surf.gecko.server.anticheat.report.AntiCheatDisplay
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.permission.PermissionList
import net.minestom.server.entity.Player
import java.util.Locale

fun antiCheatCommand() = commandTree("anticheat") {
    withAliases("ac")
    withPermission(PermissionList.COMMAND_ANTICHEAT)

    literalArgument("alerts") {
        playerExecutor { player, _ ->
            val enabled = AntiCheatDisplay.toggleAlerts(player)

            player.sendText {
                appendSuccessPrefix()
                success("Anticheat Meldungen wurden ")
                variableValue(if (enabled) "aktiviert" else "deaktiviert")
                success(".")

                if (enabled) {
                    appendNewline()
                    geckoPrimary("Die Actionbar meldet sich, sobald ein Check anschlägt. ")
                    geckoPrimary("Für eine dauerhafte Anzeige: ")
                    variableValue("/anticheat debug")
                }
            }
        }
    }

    literalArgument("debug") {
        playerExecutor { player, _ ->
            AntiCheatDisplay.watch(player, player)

            player.sendText {
                appendSuccessPrefix()
                success("Du siehst nun deine eigenen Simulationsdaten in der Actionbar.")
            }
        }

        literalArgument("stop") {
            playerExecutor { player, _ ->
                val stopped = AntiCheatDisplay.stopWatching(player)

                player.sendText {
                    if (stopped) {
                        appendSuccessPrefix()
                        success("Die Simulationsanzeige wurde beendet.")
                    } else {
                        appendErrorPrefix()
                        error("Du beobachtest gerade niemanden.")
                    }
                }
            }
        }

        playerArgument("target") {
            playerExecutor { player, arguments ->
                val target: Player by arguments

                AntiCheatDisplay.watch(player, target)

                player.sendText {
                    appendSuccessPrefix()
                    success("Du siehst nun die Simulationsdaten von ")
                    variableValue(target.username)
                    success(".")
                }
            }
        }
    }

    literalArgument("info") {
        playerArgument("target") {
            anyExecutor { sender, arguments ->
                val target: Player by arguments
                val tracked = AntiCheatTracker.find(target)

                if (tracked == null) {
                    sender.sendText {
                        appendErrorPrefix()
                        error("Für ")
                        variableValue(target.username)
                        error(" liegen keine Anticheat Daten vor.")
                    }

                    return@anyExecutor
                }

                val levels = tracked.violations.levels()
                val counts = tracked.violations.counts()

                sender.sendText {
                    appendSuccessPrefix()
                    success("Verstöße von ")
                    variableValue(target.username)
                    success(":")

                    if (levels.isEmpty()) {
                        appendNewline()
                        geckoPrimary("Keine offenen Verstöße.")
                    }

                    for ((type, level) in levels) {
                        appendNewline()
                        geckoPrimary(type.displayName)
                        appendSpace()
                        variableValue("x${counts[type] ?: 0}")
                        appendSpace()
                        spacer("(VL ")
                        spacer("%.1f".format(Locale.ROOT, level))
                        spacer(")")
                    }
                }
            }
        }
    }

    literalArgument("reset") {
        playerArgument("target") {
            anyExecutor { sender, arguments ->
                val target: Player by arguments

                AntiCheatTracker.find(target)?.violations?.reset()

                sender.sendText {
                    appendSuccessPrefix()
                    success("Die Verstöße von ")
                    variableValue(target.username)
                    success(" wurden zurückgesetzt.")
                }
            }
        }
    }
}
