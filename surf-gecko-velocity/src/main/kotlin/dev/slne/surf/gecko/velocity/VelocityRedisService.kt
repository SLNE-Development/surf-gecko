package dev.slne.surf.gecko.velocity

import dev.slne.surf.gecko.velocity.handler.GeckoVelocityRedisHandler
import dev.slne.surf.redis.RedisApi

object VelocityRedisService {
    lateinit var redisApi: RedisApi

    fun connect() {
        redisApi = RedisApi.create()
        redisApi.subscribeToEvents(GeckoVelocityRedisHandler)
        redisApi.freezeAndConnect()
    }

    fun disconnect() {
        redisApi.disconnect()
    }
}

val redisApi get() = VelocityRedisService.redisApi