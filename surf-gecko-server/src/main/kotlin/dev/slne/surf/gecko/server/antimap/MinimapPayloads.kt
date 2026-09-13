package dev.slne.surf.gecko.server.antimap

import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream

internal const val XAERO_MINIMAP_CHANNEL = "xaerominimap:main"
internal const val XAERO_WORLDMAP_CHANNEL = "xaeroworldmap:main"
internal const val VOXELMAP_SETTINGS_CHANNEL = "voxelmap:settings"
internal const val JOURNEYMAP_VERSION_CHANNEL = "journeymap:version"
internal const val JOURNEYMAP_PERMISSION_CHANNEL = "journeymap:perm_req"

internal val xaeroHandshake = payload {
    writeByte(1)
    writeInt(2)
}

internal val xaeroSettings = payload {
    writeByte(4)

    BinaryTagIO.writer().writeNameless(
        CompoundBinaryTag.builder()
            .putBoolean("cm", true)
            .putBoolean("ncm", true)
            .putBoolean("r", false)
            .build(),
        this as DataOutput
    )
}

internal val voxelSettings = payload {
    writeByte(42)
    writeUtf(
        """
        {
          "radarAllowed": false,
          "radarMobsAllowed": false,
          "radarPlayersAllowed": false,
          "cavesAllowed": true,
          "teleportCommand": ""
        }
        """.trimIndent()
    )
}

internal val journeymapVersion = payload {
    writeUtf(
        """
        {
          "journeymap_version": {
            "full": "6.1.0",
            "major": 6,
            "minor": 1,
            "micro": 0,
            "patch": ""
          },
          "loader": "minestom",
          "loader_version": "26.2",
          "minecraft_version": "26.2"
        }
        """.trimIndent()
    )
}

internal val journeymapPermissions = payload {
    writeByte(42)
    writeBoolean(false)
    writeUtf(
        """
        {
          "journeymapEnabled": "true",
          "useWorldId": "false",
          "viewOnlyServerProperties": "true",
          "allowMultiplayerSettings": "NONE",
          "worldPlayerRadar": "NONE",
          "worldPlayerRadarUpdateTime": "5",
          "seeUndergroundPlayers": "NONE",
          "hideOps": "true",
          "hideSpectators": "true",
          "allowDeathPoints": "true",
          "showInGameBeacons": "true",
          "allowWaypoints": "true",
          "allowRightClickTeleport": "false",
          "radarLateralDistance": "0",
          "radarVerticalDistance": "0",
          "maxAnimalsData": "0",
          "maxAmbientCreaturesData": "0",
          "maxMobsData": "0",
          "maxPlayersData": "0",
          "maxVillagersData": "0",
          "teleportEnabled": "false",
          "crossDimTeleport": "false",
          "renderRange": "0",
          "surfaceRenderRange": "0",
          "caveRenderRange": "0",
          "surfaceMapping": "ALL",
          "topoMapping": "ALL",
          "biomeMapping": "ALL",
          "caveMapping": "ALL",
          "radarEnabled": "NONE",
          "playerRadarEnabled": "false",
          "playerRadarNamesEnabled": "false",
          "villagerRadarEnabled": "false",
          "animalRadarEnabled": "false",
          "mobRadarEnabled": "false",
          "configVersion": "6.1.0"
        }
        """.trimIndent()
    )
    writeBoolean(true)
}

private fun payload(block: DataOutputStream.() -> Unit): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use(block)

    return bytes.toByteArray()
}

private fun DataOutputStream.writeUtf(value: String) {
    val encoded = value.toByteArray()
    var length = encoded.size

    while (length and -128 != 0) {
        writeByte(length and 127 or 128)
        length = length ushr 7
    }

    writeByte(length)
    write(encoded)
}
