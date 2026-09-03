package dev.slne.surf.gecko.server.util

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
