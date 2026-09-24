package dev.slne.surf.gecko.server.gecko.events

import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.settings.GeckoGameSettings
import java.util.*

sealed interface GeckoEvent {
    val type: Type
    fun mapData(): Map<String, String>

    data class GameStart(
        val game: GeckoGame
    ) : GeckoEvent {
        override val type = Type.GAME_START
        override fun mapData() = mutableMapOf(
            "game_id" to game.internalId.toString(),
            "players" to game.players.joinToString(",") { it.uuid.toString() }
        ).apply {
            putAll(game.settings.mapData())
        }
    }

    data class JoinMe(
        val senderUuid: UUID,
        val gameId: ULong
    ) : GeckoEvent {
        override val type = Type.JOIN_ME
        override fun mapData() = mapOf(
            "sender_uuid" to senderUuid.toString(),
            "game_id" to gameId.toString()
        )
    }

    data class CreateGame(
        val senderUuid: UUID,
        val settings: GeckoGameSettings
    ) : GeckoEvent {
        override val type = Type.CREATE_GAME
        override fun mapData() = mutableMapOf(
            "sender_uuid" to senderUuid.toString(),
        ).apply {
            putAll(settings.mapData())
        }
    }

    enum class Type {
        GAME_START,
        JOIN_ME,
        CREATE_GAME
    }
}