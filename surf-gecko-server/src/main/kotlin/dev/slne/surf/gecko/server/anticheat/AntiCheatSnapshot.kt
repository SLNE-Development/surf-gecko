package dev.slne.surf.gecko.server.anticheat

import dev.slne.surf.gecko.server.anticheat.simulation.MovementExemption

data class AntiCheatSnapshot(
    val offset: Double,
    val horizontalExcess: Double,
    val verticalExcess: Double,
    val speed: Double,
    val claimedGround: Boolean,
    val simulatedGround: Boolean,
    val exemption: MovementExemption?,
    val violationLevel: Double,
) {
    companion object {
        val EMPTY = AntiCheatSnapshot(
            offset = 0.0,
            horizontalExcess = 0.0,
            verticalExcess = 0.0,
            speed = 0.0,
            claimedGround = false,
            simulatedGround = false,
            exemption = null,
            violationLevel = 0.0,
        )
    }
}
