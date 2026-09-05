package dev.slne.surf.gecko.server.gecko.sound

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.sound
import net.kyori.adventure.sound.Sound

object GeckoSounds {
    const val COUNTDOWN_SECONDS = 3

    val SEARCH_START = sound {
        type(key("minecraft:item.goat_horn.sound.1"))
        source(Sound.Source.MASTER)
        volume(1f)
        pitch(1.3f)
    }

    val COUNTDOWN_FINISHED = sound {
        type(key("minecraft:block.note_block.bell"))
        source(Sound.Source.MASTER)
        volume(1f)
        pitch(1.5f)
    }

    val KILL_CONFIRM = sound {
        type(key("minecraft:entity.player.levelup"))
        source(Sound.Source.PLAYER)
        volume(0.7f)
        pitch(1.8f)
    }

    val KILL_CRIT = sound {
        type(key("minecraft:entity.player.attack.crit"))
        source(Sound.Source.PLAYER)
        volume(1f)
        pitch(1f)
    }

    val DEATH_SELF = sound {
        type(key("minecraft:entity.wither.hurt"))
        source(Sound.Source.MASTER)
        volume(1f)
        pitch(0.8f)
    }

    val DEATH_BROADCAST = sound {
        type(key("minecraft:block.note_block.bass"))
        source(Sound.Source.MASTER)
        volume(0.6f)
        pitch(0.6f)
    }

    fun countdownTick(secondsLeft: Int) = sound {
        type(key("minecraft:block.note_block.pling"))
        source(Sound.Source.MASTER)
        volume(1f)
        pitch(1f + (COUNTDOWN_SECONDS - secondsLeft).coerceIn(0, COUNTDOWN_SECONDS) * 0.2f)
    }

    fun heartbeat(volume: Float, pitch: Float) = sound {
        type(key("minecraft:entity.warden.heartbeat"))
        source(Sound.Source.MASTER)
        volume(volume)
        pitch(pitch)
    }
}
