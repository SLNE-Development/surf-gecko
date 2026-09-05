package dev.slne.surf.gecko.map.creator.export

import dev.slne.surf.gecko.map.creator.draft.DraftPos
import dev.slne.surf.gecko.map.creator.draft.GeckoMapDraft
import dev.slne.surf.gecko.map.creator.draft.GeckoPoiType
import java.time.OffsetDateTime
import java.util.*

object GeckoMapCodeGenerator {
    fun generate(draft: GeckoMapDraft, submittedAt: OffsetDateTime = OffsetDateTime.now()) =
        buildString {
            appendLine("GeckoMap(")
            appendLine("    mapUuid = UUID.fromString(\"${draft.mapUuid}\"),")
            appendLine("    mapName = \"${escape(draft.mapName)}\",")
            appendLine("    mapDisplayName = \"${escape(draft.mapDisplayName)}\",")
            appendLine("    mapAuthors = listOf(")
            for (author in draft.authors) {
                appendLine(
                    "        GeckoMap.MapAuthor(\"${escape(author.name)}\", " +
                            "UUID.fromString(\"${author.uuid}\")),"
                )
            }
            appendLine("    ),")
            appendLine("    mapLocations = GeckoMap.MapLocations(")
            appendLine("        lobbySpawn = ${pos(draft.single(GeckoPoiType.LOBBY_SPAWN), true)},")
            appendLine("        seekerSpawn = ${pos(draft.single(GeckoPoiType.SEEKER_SPAWN), true)},")
            appendLine("        spawn = ${pos(draft.single(GeckoPoiType.SPAWN), true)},")
            appendLine("        orbSpawns = listOf(")
            for (orbSpawn in draft.locations(GeckoPoiType.ORB_SPAWN)) {
                appendLine("            ${pos(orbSpawn, false)},")
            }
            appendLine("        ),")
            appendLine("    ),")
            appendLine("    submittedAt = OffsetDateTime.parse(\"$submittedAt\"),")
            append(")")
        }

    private fun pos(pos: DraftPos?, withRotation: Boolean): String {
        if (pos == null) return "Pos.ZERO"

        return if (withRotation) {
            "Pos(${decimal(pos.x)}, ${decimal(pos.y)}, ${decimal(pos.z)}, " +
                    "${float(pos.yaw)}, ${float(pos.pitch)})"
        } else {
            "Pos(${decimal(pos.x)}, ${decimal(pos.y)}, ${decimal(pos.z)})"
        }
    }

    private fun decimal(value: Double) = trim("%.2f".format(Locale.ROOT, value))

    private fun float(value: Float) = trim("%.2f".format(Locale.ROOT, value)) + "f"

    private fun trim(raw: String): String {
        if (!raw.contains('.')) return "$raw.0"

        val trimmed = raw.trimEnd('0')
        return if (trimmed.endsWith('.')) trimmed + "0" else trimmed
    }

    private fun escape(value: String) = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\$", "\\\$")
}
