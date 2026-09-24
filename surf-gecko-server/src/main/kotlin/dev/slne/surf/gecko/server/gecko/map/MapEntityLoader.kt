package dev.slne.surf.gecko.server.gecko.map

import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.adventure.serializer.nbt.NbtComponentSerializer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists

class MapEntityLoader(
    worldPath: Path,
    private val loadedTypes: List<EntityType>,
) {
    private val entitiesPath = worldPath.resolve("dimensions/minecraft/overworld/entities")
        .takeIf { it.exists() }
        ?: worldPath.resolve("entities")

    private val loadedChunks = ConcurrentHashMap.newKeySet<Long>()

    fun install(instance: Instance) {
        instance.eventNode().addListener(InstanceChunkLoadEvent::class.java) { event ->
            val chunkKey = (event.chunkX.toLong() shl 32) or (event.chunkZ.toLong() and 0xFFFFFFFFL)
            if (!loadedChunks.add(chunkKey)) return@addListener

            val chunkData = readChunk(event.chunkX, event.chunkZ) ?: return@addListener
            chunkData.getList("Entities", BinaryTagTypes.COMPOUND).forEach { tag ->
                val data = tag as CompoundBinaryTag
                val type = EntityType.fromKey(data.getString("id")) ?: return@forEach
                if (type !in loadedTypes) return@forEach

                createEntity(type, data)?.setInstance(instance, data.position())
            }
        }
    }

    private fun readChunk(chunkX: Int, chunkZ: Int): CompoundBinaryTag? {
        val regionFile = entitiesPath.resolve("r.${chunkX shr 5}.${chunkZ shr 5}.mca")
        if (!regionFile.exists()) return null

        RandomAccessFile(regionFile.toFile(), "r").use { file ->
            file.seek(((chunkX and 31) + (chunkZ and 31) * 32) * 4L)
            val sector = file.readInt() ushr 8
            if (sector == 0) return null

            file.seek(sector * 4096L)
            val length = file.readInt()
            val compression = when (file.readByte().toInt()) {
                1 -> BinaryTagIO.Compression.GZIP
                2 -> BinaryTagIO.Compression.ZLIB
                else -> BinaryTagIO.Compression.NONE
            }
            val data = ByteArray(length - 1)
            file.readFully(data)

            return BinaryTagIO.unlimitedReader().read(data.inputStream(), compression)
        }
    }

    private fun createEntity(type: EntityType, data: CompoundBinaryTag): Entity? {
        val entity = Entity(type)
        entity.setNoGravity(true)

        when (type) {
            EntityType.ITEM_DISPLAY -> {
                val item = data.getCompound("item").takeIf { it.size() > 0 } ?: return null
                entity.editEntityMeta<ItemDisplayMeta> { meta ->
                    meta.itemStack = ItemStack.fromItemNBT(item)
                    meta.displayContext = ItemDisplayMeta.DisplayContext.entries.find {
                        it.name.replace("_", "").equals(data.getString("item_display").replace("_", ""), true)
                    } ?: ItemDisplayMeta.DisplayContext.NONE
                }
            }

            EntityType.TEXT_DISPLAY -> entity.editEntityMeta<TextDisplayMeta> { meta ->
                data.get("text")?.let { meta.text = NbtComponentSerializer.nbt().deserialize(it) }
                meta.lineWidth = data.getInt("line_width", 200)
                meta.backgroundColor = data.getInt("background", 0x40000000)
                meta.textOpacity = data.getByte("text_opacity", -1)
                meta.isShadow = data.getBoolean("shadow")
                meta.isSeeThrough = data.getBoolean("see_through")
                meta.isUseDefaultBackground = data.getBoolean("default_background")
                meta.alignment = TextDisplayMeta.Alignment.entries.find {
                    it.name.equals(data.getString("alignment"), true)
                } ?: TextDisplayMeta.Alignment.CENTER
            }

            else -> return null
        }

        entity.editEntityMeta<AbstractDisplayMeta> { meta -> applyDisplayData(meta, data) }
        return entity
    }

    private fun applyDisplayData(meta: AbstractDisplayMeta, data: CompoundBinaryTag) {
        meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.entries.find {
            it.name.equals(data.getString("billboard"), true)
        } ?: AbstractDisplayMeta.BillboardConstraints.FIXED
        meta.viewRange = data.getFloat("view_range", 1f)
        meta.shadowRadius = data.getFloat("shadow_radius")
        meta.shadowStrength = data.getFloat("shadow_strength", 1f)
        meta.width = data.getFloat("width")
        meta.height = data.getFloat("height")
        data.getInt("glow_color_override", -1).takeIf { it != -1 }?.let { meta.glowColorOverride = it }

        val brightness = data.getCompound("brightness")
        if (brightness.size() > 0) {
            meta.setBrightness(brightness.getInt("block"), brightness.getInt("sky"))
        }

        val transformation = data.getCompound("transformation")
        transformation.floats("translation")?.let { meta.translation = Vec(it[0].toDouble(), it[1].toDouble(), it[2].toDouble()) }
        transformation.floats("scale")?.let { meta.scale = Vec(it[0].toDouble(), it[1].toDouble(), it[2].toDouble()) }
        transformation.floats("left_rotation")?.let { meta.leftRotation = it }
        transformation.floats("right_rotation")?.let { meta.rightRotation = it }
    }

    private fun CompoundBinaryTag.position(): Pos {
        val pos = getList("Pos", BinaryTagTypes.DOUBLE)
        val rotation = getList("Rotation", BinaryTagTypes.FLOAT)
        return Pos(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2), rotation.getFloat(0), rotation.getFloat(1))
    }

    private fun CompoundBinaryTag.floats(key: String): FloatArray? {
        val list = getList(key, BinaryTagTypes.FLOAT)
        if (list.size() == 0) return null
        return FloatArray(list.size()) { list.getFloat(it) }
    }
}
