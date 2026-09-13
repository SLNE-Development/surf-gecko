package dev.slne.surf.gecko.server.gecko.shop.effect.beam

import net.kyori.adventure.util.RGBLike
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle

class ParticleBeam(
    color: RGBLike,
    private val height: Double,
    private val step: Double = 0.4,
    scale: Float = 1.5f
) {
    private val particle = Particle.DUST.withProperties(color, scale)
    private val offsets = (0..(height / step).toInt()).map { it * step }

    fun packetsAt(origin: Point): List<SendablePacket> = offsets.map {
        ParticlePacket(particle, true, true, origin.add(0.0, it, 0.0), Vec.ZERO, 0f, 1)
    }
}