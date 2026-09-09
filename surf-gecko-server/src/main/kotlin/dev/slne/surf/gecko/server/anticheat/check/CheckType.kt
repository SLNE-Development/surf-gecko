package dev.slne.surf.gecko.server.anticheat.check

enum class CheckType(val displayName: String, val mitigable: Boolean) {
    PREDICTION("Simulation", true),
    SPEED("Speed", true),
    FLIGHT("Flug", true),
    NO_SLOW("NoSlow", true),
    NO_WEB("NoWeb", true),
    INVENTORY_MOVE("InvMove", true),
    GROUND_SPOOF("Bodenlüge", true),
    PHASE("Phase", true),
    TIMER("Timer", false),
    REACH("Reichweite", false),
}
