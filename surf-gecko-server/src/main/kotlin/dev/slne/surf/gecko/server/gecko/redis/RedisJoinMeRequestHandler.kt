package dev.slne.surf.gecko.server.gecko.redis

import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.common.server.SurfServer
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisRequest
import dev.slne.surf.gecko.common.joinme.AcceptJoinMeRedisResponse
import dev.slne.surf.gecko.server.database.repository.GeckoPunishmentRepository
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.redis.request.HandleRedisRequest
import dev.slne.surf.redis.request.RequestContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minestom.server.MinecraftServer

object RedisJoinMeRequestHandler {
    @HandleRedisRequest
    fun handleAcceptJoinMeRequest(context: RequestContext<AcceptJoinMeRedisRequest>) =
        context.launch {
            val playerUuid = context.request.whoWillJoin
            val surfPlayer = SurfCoreApi.getPlayer(playerUuid)
            val gameId = context.request.gameId
            val game = GeckoGameManager.findGame(gameId)

            withContext(Dispatchers.IO) {
                if (context.request.server.uuid != SurfServer.current().uuid) {
                    return@withContext
                }

                if (game == null) {
                    context.respond(
                        AcceptJoinMeRedisResponse(
                            gameId,
                            AcceptJoinMeRedisResponse.Result.GAME_NOT_FOUND
                        )
                    )
                    return@withContext
                }

                if (!game.joinable) {
                    context.respond(
                        AcceptJoinMeRedisResponse(
                            gameId,
                            AcceptJoinMeRedisResponse.Result.GAME_NOT_JOINABLE
                        )
                    )
                    return@withContext
                }

                if (GeckoPunishmentRepository.fetchActivePunishment(playerUuid) != null) {
                    context.respond(
                        AcceptJoinMeRedisResponse(
                            gameId,
                            AcceptJoinMeRedisResponse.Result.GAME_BANNED
                        )
                    )
                    return@withContext
                }

                if (surfPlayer == null) {
                    context.respond(
                        AcceptJoinMeRedisResponse(
                            gameId,
                            AcceptJoinMeRedisResponse.Result.PLAYER_NOT_FOUND
                        )
                    )
                    return@withContext
                }

                var player =
                    MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(surfPlayer.uuid)

                if (player == null) {
                    if (!surfPlayer.sendAwaiting(SurfServer.current()).isSuccessful()) {
                        context.respond(
                            AcceptJoinMeRedisResponse(
                                gameId,
                                AcceptJoinMeRedisResponse.Result.FAILED_TO_MOVE
                            )
                        )
                        return@withContext
                    }

                    player = MinecraftServer.getConnectionManager()
                        .getOnlinePlayerByUuid(surfPlayer.uuid)

                    if (player == null) {
                        context.respond(
                            AcceptJoinMeRedisResponse(
                                gameId,
                                AcceptJoinMeRedisResponse.Result.PLAYER_NOT_FOUND
                            )
                        )
                        return@withContext
                    }
                }

                if (GeckoGameManager.joinGame(player, game) == null) {
                    context.respond(
                        AcceptJoinMeRedisResponse(
                            gameId,
                            AcceptJoinMeRedisResponse.Result.GAME_NOT_JOINABLE
                        )
                    )
                    return@withContext
                }

                context.respond(
                    AcceptJoinMeRedisResponse(
                        gameId,
                        AcceptJoinMeRedisResponse.Result.SUCCESS
                    )
                )
            }
        }
}