package dev.slne.surf.gecko.server.util

import dev.slne.surf.api.core.util.random
import java.util.*

fun Int.toByteArray(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    toByte()
)

fun Long.toByteArray(): ByteArray = byteArrayOf(
    (this ushr 56).toByte(),
    (this ushr 48).toByte(),
    (this ushr 40).toByte(),
    (this ushr 32).toByte(),
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    toByte()
)

val NIL_UUID: UUID = UUID(0L, 0L)

fun UUID.toByteArray(): ByteArray =
    mostSignificantBits.toByteArray() + leastSignificantBits.toByteArray()

fun <T> Collection<T>.secureRandomOrNull() =
    if (isEmpty()) null else this.elementAt(random.nextInt(size))

fun <T> Collection<T>.secureRandom() = secureRandomOrNull() ?: error("List is empty!")
