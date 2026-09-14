package dev.slne.surf.gecko.common.joinme

import dev.slne.surf.api.core.serializer.adventure.component.SerializableComponent
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.redis.event.RedisEvent
import kotlinx.serialization.Serializable

@Serializable
data class PublishJoinMeRedisEvent(
    val geckoServer: SurfServer,
    val whoIsPlaying: SerializableComponent,
    val gameId: ULong,
    val message: SerializableComponent
) : RedisEvent()