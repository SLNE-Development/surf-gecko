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

    val PHASE_GAME = sound {
        type(key("minecraft:entity.evoker.prepare_summon"))
        pitch(0.8f)
    }

    val VENT_ENTER = sound {
        type(key("minecraft:block.iron_door.open"))
        pitch(0.6f)
    }

    val LOBBY_RISEUP = sound {
        type(key("minecraft:entity.firework_rocket.launch"))
    }

    val ORB_PICKUP = sound {
        type(key("minecraft:entity.experience_orb.pickup"))
        pitch(1.4f)
    }

    val SHOP_BUY = sound {
        type(key("minecraft:entity.player.levelup"))
        pitch(1.5f)
    }

    val SHOP_DENY = sound {
        type(key("minecraft:block.note_block.bass"))
        pitch(0.6f)
    }

    val SHOP_SONAR_PING = sound {
        type(key("minecraft:block.note_block.pling"))
        pitch(1.6f)
    }

    val SONAR_WAVE = sound {
        type(key("sonar:sonar.ping"))
        source(Sound.Source.PLAYER)
        volume(2f)
    }

    val SONAR_FOUND = sound {
        type(key("minecraft:block.note_block.bit"))
        volume(0.7f)
        pitch(1.6f)
    }

    val SHOP_SPY_DEVICE = sound {
        type(key("minecraft:block.beacon.activate"))
        pitch(1.8f)
    }

    val SHOP_LASER = sound {
        type(key("minecraft:block.beacon.power_select"))
        pitch(1.2f)
    }

    val SHOP_NET_TRAP = sound {
        type(key("minecraft:entity.happy_ghast.equip"))
        pitch(2f)
    }

    val SHOP_SMOKE_BOMB = sound {
        type(key("minecraft:block.fire.extinguish"))
        source(Sound.Source.MASTER)
        volume(1f)
        pitch(0.8f)
    }

    val SHOP_SPRING = sound {
        type(key("minecraft:entity.slime.jump"))
        pitch(0.9f)
    }

    val SHOP_SHIELD = sound {
        type(key("minecraft:item.shield.block"))
        pitch(1.2f)
    }

    val SHOP_TELEPORT = sound {
        type(key("minecraft:entity.enderman.teleport"))
        pitch(1f)
    }

    val ROLE_SELECTED_SOUND = sound {
        type(key("minecraft:entity.player.levelup"))
    }

    val PHASE_GAME_TRANSITION = sound {
        type(key("sonar:role.reveal"))
    }

    fun countdownTick(secondsLeft: Int) = sound {
        type(key("minecraft:ui.button.click"))
        pitch(2f)
    }

    fun heartbeat(volume: Float, pitch: Float) = sound {
        type(key("minecraft:entity.warden.heartbeat"))
        source(Sound.Source.MASTER)
        volume(volume)
        pitch(pitch)
    }
}
