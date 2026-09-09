package dev.slne.surf.gecko.server.anticheat.simulation

import net.minestom.server.entity.Player

data class MovementInput(
    val forward: Boolean,
    val backward: Boolean,
    val left: Boolean,
    val right: Boolean,
    val jump: Boolean,
    val sneak: Boolean,
    val sprint: Boolean,
    val usingItem: Boolean,
) {
    companion object {
        fun of(player: Player): MovementInput {
            val inputs = player.inputs()

            return MovementInput(
                forward = inputs.forward(),
                backward = inputs.backward(),
                left = inputs.left(),
                right = inputs.right(),
                jump = inputs.jump(),
                sneak = inputs.shift() || player.isSneaking,
                sprint = inputs.sprint() || player.isSprinting,
                usingItem = player.isUsingItem,
            )
        }
    }
}
