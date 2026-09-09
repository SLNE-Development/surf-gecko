package dev.slne.surf.gecko.server.anticheat.simulation

enum class MovementExemption(val displayName: String) {
    NO_INSTANCE("kein Instanzkontext"),
    UNLOADED_CHUNK("Chunk nicht geladen"),
    LAVA("Lava"),
    SPECIAL_BLOCK("Spezialblock"),
    ELYTRA("Elytra"),
    VEHICLE("Fahrzeug"),
    LEVITATION("Levitation"),
    GAME_MODE("Spielmodus"),
    TELEPORT("Teleport"),
    KNOCKBACK("Rückstoß"),
    JOINING("Spawnphase"),
}
