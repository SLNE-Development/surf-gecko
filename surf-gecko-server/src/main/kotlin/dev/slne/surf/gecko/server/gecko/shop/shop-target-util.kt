package dev.slne.surf.gecko.server.gecko.shop

import dev.slne.surf.gecko.server.gecko.GeckoGame
import dev.slne.surf.gecko.server.gecko.player.game.GeckoGameRole
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance

internal fun GeckoGame.activePlayers(role: GeckoGameRole) = gamePlayers
    .filter { it.role == role && !it.awaitingRespawn }
    .mapNotNull { it.playerOrNull }

internal fun GeckoGame.activeHiders() = activePlayers(GeckoGameRole.HIDER)

internal fun GeckoGame.activeSeekers() = activePlayers(GeckoGameRole.SEEKER)

internal fun List<Player>.otherThan(player: Player) =
    filter { it.uuid != player.uuid && it.instance == player.instance }

internal fun List<Player>.nearestTo(player: Player) = otherThan(player)
    .minByOrNull { it.position.distance(player.position) }

internal fun List<Player>.within(player: Player, radius: Double) = otherThan(player)
    .filter { it.position.distance(player.position) <= radius }

internal fun List<Player>.within(instance: Instance, position: Point, radius: Double) =
    filter { it.instance == instance && it.position.distance(position) <= radius }

