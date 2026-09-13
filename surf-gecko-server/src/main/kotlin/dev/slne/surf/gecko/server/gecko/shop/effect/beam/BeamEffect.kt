package dev.slne.surf.gecko.server.gecko.shop.effect.beam

import dev.slne.surf.gecko.server.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.shop.effect.GeckoEffect
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player

class BeamEffect(
    private val viewers: List<Player>,
    origins: Collection<Point>,
    beam: ParticleBeam
) : GeckoEffect(8.ticks) {

    private val packets = origins.flatMap(beam::packetsAt)

    override fun isActive() = packets.isNotEmpty()
    override fun start() = Unit

    override fun pulse() = viewers.forEach { if (it.isOnline) it.sendPackets(packets) }
    override fun stop() = Unit
}
