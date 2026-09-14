package dev.slne.surf.gecko.velocity.handler

import com.github.shynixn.mccoroutine.velocity.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisRequest
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisResponse
import dev.slne.surf.gecko.common.joinme.PublishJoinMeRedisEvent
import dev.slne.surf.gecko.velocity.plugin
import dev.slne.surf.gecko.velocity.proxy
import dev.slne.surf.gecko.velocity.redisApi
import dev.slne.surf.redis.event.OnRedisEvent

object GeckoVelocityRedisHandler {
    @OnRedisEvent
    fun onJoinMePublish(event: PublishJoinMeRedisEvent) {
        proxy.allPlayers.forEach {
            it.sendText {
                append(event.message)
                clickCallback { audience ->
                    val uuid = audience.uuidOrNull() ?: return@clickCallback

                    plugin.pluginContainer.launch {
                        val response = runCatching {
                            redisApi.sendRequest<AcceptJoinMeRedisResponse>(
                                AcceptJoinMeRedisRequest(
                                    event.geckoServer,
                                    uuid,
                                    event.gameId
                                )
                            )
                        }.getOrNull()

                        if (response == null || !response.result.isSuccess()) {
                            audience.sendText {
                                appendErrorPrefix()
                                error("Du konntest dem Spiel nicht beitreten. Grund: ${response?.result ?: "Das JoinMe ist abgelaufen."}")
                            }
                            return@launch
                        }
                    }
                }
            }
        }
    }
}