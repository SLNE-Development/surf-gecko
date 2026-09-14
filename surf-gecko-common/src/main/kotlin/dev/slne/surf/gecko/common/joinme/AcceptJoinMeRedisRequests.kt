package dev.slne.surf.gecko.common.joinme

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.redis.request.RedisRequest
import dev.slne.surf.redis.request.RedisResponse
import kotlinx.serialization.Serializable

@Serializable
data class AcceptJoinMeRedisRequest(
    val server: SurfServer,
    val whoWillJoin: SerializableUUID,
    val gameId: ULong
) : RedisRequest()

@Serializable
data class AcceptJoinMeRedisResponse(
    val gameId: ULong,
    val result: Result
) : RedisResponse() {
    enum class Result {
        SUCCESS,
        PLAYER_NOT_FOUND,
        FAILED_TO_MOVE,
        GAME_NOT_FOUND,
        GAME_NOT_JOINABLE,
        GAME_BANNED;

        fun isSuccess(): Boolean {
            return this == SUCCESS
        }
    }
}