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
                    appendSuccessPrefix()
                    success("Anzeigename gesetzt: ")
                    variableValue(draft.mapDisplayName)
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
                            appendErrorPrefix()
                            variableValue(target.name)
                            error(" ist bereits Autor.")
                        }
                        return@playerExecutor
                    }

                    draft.authors.add(
                        GeckoMapDraft.DraftAuthor(target.name, target.uniqueId)
                    )

                    player.sendText {
                        appendSuccessPrefix()
                        success("Autor hinzugefügt: ")
                        variableValue(target.name)
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
                            appendSuccessPrefix()
                            success("Autor entfernt: ")
                            variableValue(name)
                        } else {
                            appendErrorPrefix()
                            error("Kein Autor mit dem Namen ")
                            variableValue(name)
                            error(" gefunden.")
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
                        appendErrorPrefix()
                        error("Unbekannter POI. Möglich: ")
                        variableValue(GeckoPoiType.entries.joinToString { it.id })
                    }
                    return@playerExecutor
                }

                val removed = draft.clear(type)
                MarkerRenderer.refresh(player)

                player.sendText {
                    appendSuccessPrefix()
                    variableValue(removed)
                    success(" Marker von ")
                    variableValue(type.displayName)
                    success(" entfernt.")
                }
            }
        }
    }

    literalArgument("tools") {
        playerExecutor { player, _ ->
            player.requireDraft() ?: return@playerExecutor
            MarkerItems.giveTools(player)

            player.sendText {
                appendSuccessPrefix()
                success("Marker-Items in die Hotbar gelegt.")
            }
        }
    }

    literalArgument("markers") {
        playerExecutor { player, _ ->
            val enabled = MarkerRenderer.toggle(player)

            player.sendText {
                appendInfoPrefix()
                info("Marker-Anzeige ")
                if (enabled) success("aktiviert") else warning("deaktiviert")
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
                    appendErrorPrefix()
                    error("Es fehlen noch POIs: ")
                    variableValue(missing.joinToString { it.displayName })
                }
                return@playerExecutorSuspend
            }

            val code = GeckoMapCodeGenerator.generate(draft)
            val url = runCatching { PasteService.upload(code) }.getOrElse { throwable ->
                log.atSevere()
                    .withCause(throwable)
                    .log("Failed to upload map %s to pastes.dev", draft.mapName)

                player.sendText {
                    appendErrorPrefix()
                    error("Upload fehlgeschlagen: ")
                    variableValue(throwable.message ?: throwable::class.simpleName ?: "unbekannt")
                }
                return@playerExecutorSuspend
            }

            player.sendText {
                appendSuccessPrefix()
                success("Map exportiert - ")
                append {
                    variableValue(url, TextDecoration.UNDERLINED)
                    clickOpensUrl(url)
                }
                spacer(" ")
                append {
                    info("[Kopieren]")
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
                    appendSuccessPrefix()
                    success("Map verworfen.")
                } else {
                    appendErrorPrefix()
                    error("Du hast keine aktive Map.")
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
            variableValue("/geckomap create <name>")
            info(".")
        }
        return
    }

    sendText {
        appendInfoPrefix()
        primary(draft.mapDisplayName, TextDecoration.BOLD)
        spacer(" (")
        variableValue(draft.mapName)
        spacer(")")

        appendNewInfoPrefixedLine {
            variableKey("UUID: ")
            variableValue(draft.mapUuid.toString())
        }
        appendNewInfoPrefixedLine {
            variableKey("Welt: ")
            variableValue(draft.worldName)
        }
        appendNewInfoPrefixedLine {
            variableKey("Autoren: ")
            variableValue(draft.authors.joinToString { it.name })
        }

        for (type in GeckoPoiType.entries) {
            val locations = draft.locations(type)

            appendNewInfoPrefixedLine {
                text(type.displayName, type.color)
                spacer(": ")

                when {
                    locations.isEmpty() -> error("nicht gesetzt")
                    type.multiple -> {
                        variableValue(locations.size)
                        spacer(" Marker")
                    }

                    else -> variableValue(readable(locations.first()))
                }
            }
        }

        appendNewInfoPrefixedLine {
            variableKey("POIs insgesamt: ")
            variableValue(draft.totalPois())
        }
    }
}

private fun createDraft(player: Player, name: String, displayName: String?) {
    if (!NAME_PATTERN.matches(name)) {
        player.sendText {
            appendErrorPrefix()
            error("Ungültiger Map-Name. Erlaubt: ")
            variableValue("a-z, 0-9, - und _ (3-32 Zeichen)")
        }
        return
    }

    val existing = PaperGeckoMapManager.draft(player.uniqueId)
    if (existing != null) {
        player.sendText {
            appendWarningPrefix()
            warning("Die vorherige Map ")
            variableValue(existing.mapName)
            warning(" wurde verworfen.")
        }
    }

    PaperGeckoMapManager.reset(player)
    PaperGeckoMapManager.create(player, name, displayName ?: name)

    player.sendText {
        appendSuccessPrefix()
        success("Map ")
        variableValue(name)
        success(" gestartet - Marker-Items liegen in der Hotbar.")
    }
}

private fun Player.requireDraft(): GeckoMapDraft? {
    val draft = PaperGeckoMapManager.draft(uniqueId)

    if (draft == null) {
        sendText {
            appendErrorPrefix()
            error("Du hast keine aktive Map. Starte mit ")
            variableValue("/geckomap create <name>")
            error(".")
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
