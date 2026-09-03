package dev.slne.surf.gecko.server.util

import java.util.*

val NIL_UUID: UUID = UUID(0L, 0L)

fun UUID.toByteArray(): ByteArray =
    mostSignificantBits.toByteArray() + leastSignificantBits.toByteArray()
