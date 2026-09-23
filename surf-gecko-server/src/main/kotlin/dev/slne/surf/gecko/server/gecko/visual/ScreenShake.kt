package dev.slne.surf.gecko.server.gecko.visual

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.coroutine.ticks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.minestom.server.color.Color
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ScreenShake {
    private val active = ConcurrentHashMap<UUID, Job>()

    fun play(player: Player, strength: Double, durationTicks: Int = 20) {
        val instance = player.instance ?: return
        val duration = durationTicks.coerceIn(1, 255)
        val power = (strength.coerceIn(0.0, 1.0) * 255).toInt()
        val start = instance.shaderTick(2)

        val job = geckoScope.launch(start = CoroutineStart.LAZY) {
            val probe = spawnVisualProbe(
                instance,
                player.eyePosition(),
                player,
                "sonar:shake_probe",
                listOf(Color(power, start.high, start.low), Color(duration, 0x80, 0xA5)),
                cullSize = 256f,
                viewRange = 4f
            )

            try {
                withTimeoutOrNull((duration + 7).ticks) {
                    while (player.isOnline && player.instance == instance) {
                        delay(5.ticks)
                        probe.teleport(player.eyePosition())
                    }
                }
            } finally {
                probe.remove()
                active.remove(player.uuid, coroutineContext.job)
            }
        }

        active.put(player.uuid, job)?.cancel()
        job.start()
    }

    private fun Player.eyePosition() = position.add(0.0, eyeHeight, 0.0)
}
