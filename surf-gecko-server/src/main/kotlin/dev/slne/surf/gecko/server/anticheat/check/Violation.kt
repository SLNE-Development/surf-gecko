package dev.slne.surf.gecko.server.anticheat.check

data class Violation(
    val type: CheckType,
    val detail: String,
    val level: Double,
    val count: Int,
    val notable: Boolean,
)
