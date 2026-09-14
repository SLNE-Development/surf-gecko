package dev.slne.surf.gecko.server.redis

import dev.slne.surf.gecko.server.gecko.redis.RedisJoinMeRequestHandler
import dev.slne.surf.redis.RedisApi

object RedisService {
    lateinit var redisApi: RedisApi

    fun connect() {
        redisApi = RedisApi.create()
        redisApi.registerRequestHandler(RedisJoinMeRequestHandler)
        redisApi.freezeAndConnect()
    }

    fun disconnect() {
        redisApi.disconnect()
    }
}

val redisApi get() = RedisService.redisApi