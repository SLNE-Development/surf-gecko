package dev.slne.surf.gecko.common.joinme

import dev.slne.surf.api.core.serializer.adventure.component.SerializableComponent
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.redis.event.RedisEvent
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import java.util.Locale

@Serializable
data class PublishJoinMeRedisEvent(
    val geckoServer: SurfServer,
    val whoIsPlaying: SerializableComponent,
    val gameId: ULong,
    val messages: Map<String, SerializableComponent>
) : RedisEvent() {
    fun message(locale: Locale?): Component {
        val languageId = when (locale?.language) {
            null, "", "de" -> "de_de"
            else -> "en_us"
        }

        return messages[languageId] ?: messages.values.firstOrNull() ?: Component.empty()
    }
}