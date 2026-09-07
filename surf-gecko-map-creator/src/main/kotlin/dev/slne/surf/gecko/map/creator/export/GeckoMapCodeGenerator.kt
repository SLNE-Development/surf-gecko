package dev.slne.surf.gecko.map.creator.export

import dev.slne.surf.gecko.map.creator.draft.DraftPos
import dev.slne.surf.gecko.map.creator.draft.GeckoMapDraft
import dev.slne.surf.gecko.map.creator.draft.GeckoPoiType
import java.time.OffsetDateTime
import java.util.*

object GeckoMapCodeGenerator {
    private const val BASE_PACKAGE = "dev.slne.surf.gecko.server.gecko.map.maps"
    private const val SOURCE_ROOT = "surf-gecko-server/src/main/kotlin"

    fun packageName(draft: GeckoMapDraft) = "$BASE_PACKAGE.${packageSegment(draft.mapName)}"

    fun objectName(draft: GeckoMapDraft) = objectName(draft.mapName)

    fun filePath(draft: GeckoMapDraft) =
        "$SOURCE_ROOT/${packageName(draft).replace('.', '/')}/${objectName(draft)}.kt"

    fun generate(draft: GeckoMapDraft, submittedAt: OffsetDateTime = OffsetDateTime.now()) =
        buildString {
            appendLine("package ${packageName(draft)}")
            appendLine()
            appendLine("import dev.slne.surf.gecko.server.gecko.map.GeckoMap")
            appendLine("import net.minestom.server.coordinate.Pos")
            appendLine("import java.time.OffsetDateTime")
            appendLine("import java.util.*")
            appendLine()
            appendLine("object ${objectName(draft)} : GeckoMap {")
            appendLine("    override val mapUuid: UUID = UUID.fromString(\"${draft.mapUuid}\")")
            appendLine("    override val mapName = \"${escape(draft.mapName)}\"")
            appendLine("    override val mapDisplayName = \"${escape(draft.mapDisplayName)}\"")

            if (draft.authors.isEmpty()) {
                appendLine("    override val mapAuthors = listOf<GeckoMap.MapAuthor>()")
            } else {
                appendLine("    override val mapAuthors = listOf(")
                for (author in draft.authors) {
                    appendLine(
                        "        GeckoMap.MapAuthor(\"${escape(author.name)}\", " +
                                "UUID.fromString(\"${author.uuid}\")),"
                    )
                }
                appendLine("    )")
            }

            appendLine("    override val mapLocations = GeckoMap.MapLocations(")
            appendLine("        lobbySpawn = ${pos(draft, GeckoPoiType.LOBBY_SPAWN)},")
            appendLine("        seekerSpawn = ${pos(draft, GeckoPoiType.SEEKER_SPAWN)},")
            appendLine("        spawn = ${pos(draft, GeckoPoiType.SPAWN)},")

            val orbSpawns = draft.locations(GeckoPoiType.ORB_SPAWN)
            if (orbSpawns.isEmpty()) {
                appendLine("        orbSpawns = listOf(),")
            } else {
                appendLine("        orbSpawns = listOf(")
                for (orbSpawn in orbSpawns) {
                    appendLine("            ${pos(orbSpawn, GeckoPoiType.ORB_SPAWN.withRotation)},")
                }
                appendLine("        ),")
            }

            appendLine("    )")
            appendLine(
                "    override val submittedAt: OffsetDateTime = " +
                        "OffsetDateTime.parse(\"$submittedAt\")"
            )
            append("}")
        }

    private fun packageSegment(mapName: String): String {
        val segment = mapName
            .removeSuffix("-map")
            .removeSuffix("_map")
            .filter { it.isLetterOrDigit() }
            .lowercase()

        return segment.ifEmpty { "map" }.let { if (it.first().isDigit()) "map$it" else it }
    }

    private fun objectName(mapName: String): String {
        val pascal = mapName
            .split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.filter { it.isLetterOrDigit() }
                    .replaceFirstChar { it.uppercaseChar() }
            }
            .ifEmpty { "Gecko" }
            .let { if (it.first().isDigit()) "Map$it" else it }

        return if (pascal.endsWith("Map")) pascal else "${pascal}Map"
    }

    private fun pos(draft: GeckoMapDraft, type: GeckoPoiType) =
        pos(draft.single(type), type.withRotation)

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
        .replace("\$", "\\$")
}
