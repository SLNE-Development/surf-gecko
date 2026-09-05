package dev.slne.surf.gecko.map.creator.command

import com.github.shynixn.mccoroutine.folia.scope
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.gecko.map.creator.PaperGeckoMapManager
import dev.slne.surf.gecko.map.creator.draft.DraftPos
import dev.slne.surf.gecko.map.creator.draft.GeckoMapDraft
import dev.slne.surf.gecko.map.creator.draft.GeckoPoiType
import dev.slne.surf.gecko.map.creator.export.GeckoMapCodeGenerator
import dev.slne.surf.gecko.map.creator.export.PasteService
import dev.slne.surf.gecko.map.creator.item.MarkerItems
import dev.slne.surf.gecko.map.creator.permission.MapCreatorPermissions
import dev.slne.surf.gecko.map.creator.plugin
import dev.slne.surf.gecko.map.creator.render.MarkerRenderer
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.util.*

private val log = logger()
private val NAME_PATTERN = Regex("[a-z0-9-_]{3,32}")

fun geckoMapCommand() = commandTree("geckomap") {
    withPermission(MapCreatorPermissions.COMMAND)
    withAliases("gmap")

    playerExecutor { player, _ -> player.sendDraftOverview() }

    literalArgument("create") {
        stringArgument("name") {
            playerExecutor { player, arguments -> createDraft(player, arguments.name(), null) }

            greedyStringArgument("displayName") {
                playerExecutor { player, arguments ->
                    createDraft(player, arguments.name(), arguments.displayName())
                }
            }
        }
    }

    literalArgument("displayname") {
        greedyStringArgument("displayName") {
            playerExecutor { player, arguments ->
                val draft = player.requireDraft() ?: return@playerExecutor
                draft.mapDisplayName = arguments.displayName()

                player.sendText {
                    appendInfoPrefix()
                    info("Anzeigename gesetzt: ")
                    white(draft.mapDisplayName)
                }
            }
        }
    }

    literalArgument("author") {
        literalArgument("add") {
            entitySelectorArgumentOnePlayer("target") {
                playerExecutor { player, arguments ->
                    val draft = player.requireDraft() ?: return@playerExecutor
                    val target = arguments.getUnchecked<Player>("target")
                        ?: return@playerExecutor

                    if (draft.authors.any { it.uuid == target.uniqueId }) {
                        player.sendText {
                            appendInfoPrefix()
                            white(target.name)
                            info(" ist bereits Autor.")
                        }
                        return@playerExecutor
                    }

                    draft.authors.add(
                        GeckoMapDraft.DraftAuthor(target.name, target.uniqueId)
                    )

                    player.sendText {
                        appendInfoPrefix()
                        info("Autor hinzugefügt: ")
                        white(target.name)
                    }
                }
            }
        }

        literalArgument("remove") {
            stringArgument("name") {
                playerExecutor { player, arguments ->
                    val draft = player.requireDraft() ?: return@playerExecutor
                    val name = arguments.name()
                    val removed = draft.authors.removeIf { it.name.equals(name, true) }

                    player.sendText {
                        if (removed) {
                            appendInfoPrefix()
                            info("Autor entfernt: ")
                            white(name)
                        } else {
                            appendInfoPrefix()
                            info("Kein Autor mit dem Namen ")
                            white(name)
                            info(" gefunden.")
                        }
                    }
                }
            }
        }
    }

    literalArgument("clear") {
        stringArgument("poi") {
            playerExecutor { player, arguments ->
                val draft = player.requireDraft() ?: return@playerExecutor
                val type = GeckoPoiType.byId(arguments.getUnchecked<String>("poi") ?: "")

                if (type == null) {
                    player.sendText {
                        appendInfoPrefix()
                        info("Unbekannter POI. Möglich: ")
                        white(GeckoPoiType.entries.joinToString { it.id })
                    }
                    return@playerExecutor
                }

                val removed = draft.clear(type)
                MarkerRenderer.refresh(player)

                player.sendText {
                    appendInfoPrefix()
                    variableValue(removed)
                    info(" Marker von ")
                    white(type.displayName)
                    info(" entfernt.")
                }
            }
        }
    }

    literalArgument("tools") {
        playerExecutor { player, _ ->
            player.requireDraft() ?: return@playerExecutor
            MarkerItems.giveTools(player)

            player.sendText {
                appendInfoPrefix()
                info("Marker-Items in die Hotbar gelegt.")
            }
        }
    }

    literalArgument("markers") {
        playerExecutor { player, _ ->
            val enabled = MarkerRenderer.toggle(player)

            player.sendText {
                appendInfoPrefix()
                info("Marker-Anzeige ")
                white(if (enabled) "aktiviert" else "deaktiviert")
                info(".")
            }
        }
    }

    literalArgument("info") {
        playerExecutor { player, _ -> player.sendDraftOverview() }
    }

    literalArgument("export") {
        playerExecutorSuspend({ plugin.scope }) { player, _ ->
            val draft = player.requireDraft() ?: return@playerExecutorSuspend
            val missing = draft.missingTypes()

            if (missing.isNotEmpty()) {
                player.sendText {
                    appendInfoPrefix()
                    info("Es fehlen noch POIs: ")
                    white(missing.joinToString { it.displayName })
                }
                return@playerExecutorSuspend
            }

            val code = GeckoMapCodeGenerator.generate(draft)
            val url = runCatching { PasteService.upload(code) }.getOrElse { throwable ->
                log.atSevere()
                    .withCause(throwable)
                    .log("Failed to upload map %s to pastes.dev", draft.mapName)

                player.sendText {
                    appendInfoPrefix()
                    info("Upload fehlgeschlagen: ")
                    white(throwable.message ?: throwable::class.simpleName ?: "unbekannt")
                }
                return@playerExecutorSuspend
            }

            player.sendText {
                appendInfoPrefix()
                info("Map exportiert - ")
                append {
                    variableValue(url, TextDecoration.UNDERLINED)
                    clickOpensUrl(url)
                }
                info(" ")
                append {
                    primary("[Kopieren]")
                    clickCopiesToClipboard(url)
                }
            }
        }
    }

    literalArgument("reset") {
        playerExecutor { player, _ ->
            val removed = PaperGeckoMapManager.reset(player)

            player.sendText {
                if (removed) {
                    appendInfoPrefix()
                    info("Map verworfen.")
                } else {
                    appendInfoPrefix()
                    info("Du hast keine aktive Map.")
                }
            }
        }
    }
}

fun Player.sendDraftOverview() {
    val draft = PaperGeckoMapManager.draft(uniqueId)

    if (draft == null) {
        sendText {
            appendInfoPrefix()
            info("Keine aktive Map. Starte mit ")
            white("/geckomap create <name>")
            info(".")
        }
        return
    }

    sendText {
        appendInfoPrefix()
        primary(draft.mapDisplayName, TextDecoration.BOLD)
        info(" (")
        white(draft.mapName)
        info(")")

        appendNewInfoPrefixedLine {
            primary("UUID: ")
            white(draft.mapUuid.toString())
        }
        appendNewInfoPrefixedLine {
            primary("Welt: ")
            white(draft.worldName)
        }
        appendNewInfoPrefixedLine {
            primary("Autoren: ")
            white(draft.authors.joinToString { it.name })
        }

        for (type in GeckoPoiType.entries) {
            val locations = draft.locations(type)

            appendNewInfoPrefixedLine {
                primary(type.displayName)
                info(": ")

                when {
                    locations.isEmpty() -> info("nicht gesetzt")
                    type.multiple -> {
                        white(locations.size)
                        info(" Marker")
                    }

                    else -> white(readable(locations.first()))
                }
            }
        }

        appendNewInfoPrefixedLine {
            primary("POIs insgesamt: ")
            variableValue(draft.totalPois())
        }
    }
}

private fun createDraft(player: Player, name: String, displayName: String?) {
    if (!NAME_PATTERN.matches(name)) {
        player.sendText {
            appendInfoPrefix()
            info("Ungültiger Map-Name. Erlaubt: ")
            white("a-z, 0-9, - und _ (3-32 Zeichen)")
        }
        return
    }

    val existing = PaperGeckoMapManager.draft(player.uniqueId)
    if (existing != null) {
        player.sendText {
            appendInfoPrefix()
            info("Die vorherige Map ")
            white(existing.mapName)
            info(" wurde verworfen.")
        }
    }

    PaperGeckoMapManager.reset(player)
    PaperGeckoMapManager.create(player, name, displayName ?: name)

    player.sendText {
        appendInfoPrefix()
        info("Map ")
        variableValue(name)
        info(" gestartet - Marker-Items liegen in der Hotbar.")
    }
}

private fun Player.requireDraft(): GeckoMapDraft? {
    val draft = PaperGeckoMapManager.draft(uniqueId)

    if (draft == null) {
        sendText {
            appendInfoPrefix()
            info("Du hast keine aktive Map. Starte mit ")
            white("/geckomap create <name>")
            info(".")
        }
    }

    return draft
}

private fun readable(pos: DraftPos) = "%.1f / %.1f / %.1f @ %.0f°".format(
    Locale.ROOT,
    pos.x,
    pos.y,
    pos.z,
    pos.yaw,
)

private fun dev.jorel.commandapi.executors.CommandArguments.name() =
    getUnchecked<String>("name") ?: ""

private fun dev.jorel.commandapi.executors.CommandArguments.displayName() =
    getUnchecked<String>("displayName") ?: ""
