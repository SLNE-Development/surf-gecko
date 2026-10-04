package dev.slne.surf.gecko.server.gecko.visual

import dev.slne.surf.api.minestom.coroutine.minestomScope
import dev.slne.surf.api.minestom.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.shop.effect.glow.GlowEffect
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.sendTranslatedActionBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

object SonarWave {
    fun scan(initiator: Player, radius: Double, targets: List<Player>) {
        val instance = initiator.instance ?: return
        val center = initiator.position.add(0.0, 0.05, 0.0).withView(0f, 0f)
        val duration = ceil(radius / 25.0 * 20.0).toInt().coerceIn(4, 127)

        minestomScope.launch {
            launch {
                delay(2.ticks)
                instance.playSound(GeckoSounds.SONAR_WAVE, center)
            }

            targets.forEach { target ->
                launch {
                    val reach = min(target.position.distance(center), radius) / radius
                    delay((2 + (reach * duration).toInt()).ticks)

                    if (!target.isOnline || !initiator.isOnline) {
                        return@launch
                    }

                    initiator.playSound(GeckoSounds.SONAR_FOUND, Sound.Emitter.self())
                    GlowEffect(initiator, listOf(target)).playFor(5.seconds)
                }
            }

            launch {
                delay((2 + duration).ticks)
                sendSummary(initiator, targets.size, radius.toInt())
            }

            repeat(2) { pulse ->
                if (!initiator.isOnline) {
                    return@launch
                }

                val probe = spawnWaveProbe(instance, center, initiator, radius, duration, pulse)

                launch {
                    delay((duration + 22).ticks)
                    probe.remove()
                }

                delay(12.ticks)
            }
        }
    }

    private fun spawnWaveProbe(
        instance: Instance,
        center: Pos,
        viewer: Player,
        radius: Double,
        duration: Int,
        slot: Int
    ) = instance.shaderTick(2).let { start ->
        spawnVisualProbe(
            instance,
            center,
            viewer,
            "sonar:probe",
            listOf(
                Color((slot shl 7) or duration, start.high, start.low),
                Color(radius.toInt().coerceIn(1, 255), 0x80 or slot, 0)
            ),
            cullSize = (radius * 2 + 64).toFloat(),
            viewRange = ((radius + 128) / 64).toFloat()
        )
    }

    private fun sendSummary(player: Player, found: Int, range: Int) = when (found) {
        0 -> player.sendTranslatedActionBar("game.sonar.none", "range" to range)
        1 -> player.sendTranslatedActionBar("game.sonar.found.one", "found" to found, "range" to range)
        else -> player.sendTranslatedActionBar("game.sonar.found.other", "found" to found, "range" to range)
    }
}
