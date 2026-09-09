package dev.slne.surf.gecko.server.anticheat.simulation

import net.minestom.server.instance.block.Block

private const val BED_BOUNCE = 0.66
private const val SLIME_BOUNCE = 1.0

private val SLIME = Block.SLIME_BLOCK.key()

private val BEDS = setOf(
    Block.WHITE_BED.key(),
    Block.ORANGE_BED.key(),
    Block.MAGENTA_BED.key(),
    Block.LIGHT_BLUE_BED.key(),
    Block.YELLOW_BED.key(),
    Block.LIME_BED.key(),
    Block.PINK_BED.key(),
    Block.GRAY_BED.key(),
    Block.LIGHT_GRAY_BED.key(),
    Block.CYAN_BED.key(),
    Block.PURPLE_BED.key(),
    Block.BLUE_BED.key(),
    Block.BROWN_BED.key(),
    Block.GREEN_BED.key(),
    Block.RED_BED.key(),
    Block.BLACK_BED.key(),
)

object BounceBlocks {

    fun bounceOf(block: Block): Double {
        val key = block.key()

        return when {
            key == SLIME -> SLIME_BOUNCE
            key in BEDS -> BED_BOUNCE
            else -> 0.0
        }
    }

    fun isSlime(block: Block) = block.key() == SLIME
}
