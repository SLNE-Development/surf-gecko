package dev.slne.surf.gecko.velocity.handler

import com.github.shynixn.mccoroutine.velocity.launch
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisRequest
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisResponse
import dev.slne.surf.gecko.common.joinme.PublishJoinMeRedisEvent
import dev.slne.surf.gecko.velocity.plugin
import dev.slne.surf.gecko.velocity.proxy
import dev.slne.surf.gecko.velocity.redisApi
import dev.slne.surf.redis.event.OnRedisEvent
import java.util.Locale

object GeckoVelocityRedisHandler {
    @OnRedisEvent
    fun onJoinMePublish(event: PublishJoinMeRedisEvent) {
        proxy.allPlayers.forEach { player ->
            player.sendText {
                append(event.message(player.effectiveLocale))
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
                            val locale = (audience as? Player)?.effectiveLocale ?: player.effectiveLocale
                            audience.sendText {
                                appendErrorPrefix()
                                error(failureMessage(locale, response?.result))
                            }
                            return@launch
                        }
                    }
                }
            }
        }
    }

    private fun failureMessage(locale: Locale?, result: AcceptJoinMeRedisResponse.Result?): String {
        val german = locale?.language.let { it == null || it == "" || it == "de" }

        val reason = when (result) {
            null -> if (german) "Das JoinMe ist abgelaufen." else "The JoinMe has expired."
            AcceptJoinMeRedisResponse.Result.PLAYER_NOT_FOUND ->
                if (german) "Du wurdest nicht gefunden." else "You could not be found."

            AcceptJoinMeRedisResponse.Result.FAILED_TO_MOVE ->
                if (german) "Du konntest nicht auf den Server verschoben werden." else "You could not be moved to the server."

            AcceptJoinMeRedisResponse.Result.GAME_NOT_FOUND ->
                if (german) "Das Spiel existiert nicht mehr." else "The game no longer exists."

            AcceptJoinMeRedisResponse.Result.GAME_NOT_JOINABLE ->
                if (german) "Das Spiel ist voll oder läuft bereits." else "The game is full or has already started."

            AcceptJoinMeRedisResponse.Result.GAME_BANNED ->
                if (german) "Du bist aktuell gesperrt." else "You are currently banned."

            AcceptJoinMeRedisResponse.Result.SUCCESS -> result.name
        }

        return if (german) {
            "Du konntest dem Spiel nicht beitreten. Grund: $reason"
        } else {
            "You could not join the game. Reason: $reason"
        }
    }
}