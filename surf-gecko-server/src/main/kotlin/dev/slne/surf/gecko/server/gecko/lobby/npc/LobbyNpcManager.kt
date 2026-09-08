package dev.slne.surf.gecko.server.gecko.lobby.npc

import codes.bed.minestom.npc.api.NpcInteractionType
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.gecko.GeckoGameManager
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.lobby.view.geckoGamesView
import dev.slne.surf.gecko.server.gecko.util.appendPrefix
import dev.slne.surf.gecko.server.gecko.util.geckoPrimary
import dev.slne.surf.gecko.server.gecko.util.geckoSecondary
import kotlinx.coroutines.launch
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.network.player.ResolvableProfile

object LobbyNpcManager {
    fun create() {
        equipableMannequinNpc("games", GeckoLobby.instance, GeckoLobby.npcPos) {
            displayName = buildText {
                geckoPrimary("Hide 'n Seek", TextDecoration.BOLD)
                appendNewline()
                geckoSecondary("Spiel beitreten")
            }
            profile = ResolvableProfile(
                PlayerSkin(
                    "ewogICJ0aW1lc3RhbXAiIDogMTc4ODc5MDQ1ODg4OCwKICAicHJvZmlsZUlkIiA6ICI0ZWEwN2YwODlmN2U0MWZhYmMwNjRhMjZlNWM1OWU2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGlmZnRvcGlhMiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NmFkY2RiNzk3NjljYmQ4MDZhZTY1NTc1NjNjMzAzNDE1ZDEwZTA0YzIyNzliNzk0ZDRmNjUwNGRjNTlhOGU3IgogICAgfQogIH0KfQ==",
                    "yR1wX09z7Cl/04vfudjaF0ZPph/JeRcDh9txnJHIWujfjNII/5I48lpfMjwHCgMxbAeGQir/gYSRJGqzYM9/FNzjp4FiW/gflSBD9ERB2gctKpHnzNyKgeq93fJl/9g6mQZ/Z29n9SN1QMudyvLGc92Gi8TIqu5HcBQNatVA3ll1RVC4vG4UdcUtURRIfBrGWvc7Xu9jrOg6UvkGZoyIm5p+EFq/q1YNLEawnc1BWMAaTMb+CH/cTVxt3ZJ15jjIqSNmKu9ej5sgOGyXJNXsk+umArYpE08g7xkEyw969LvDaU7kQcetMtWNH4BAdLpNcTni0+Ej98KdvAE8jByURPgph9qULQjMBA/vhSVeao1otVQbSrgCFaJ145/cLF8mvrK1DNC8GZT9NmhVYQ5+h6Bkhu48mbNe6EAk43/Eu1SRwNRFzafOUddtaL9MyiV99xzPWIvXSJtUQhCr+2BeEFMLmgbqP92KLO1Zp6xr4I0w7qssArmq+HFox6Q9fncpxGug5ekUjh3m+N0xs9FT3rcql6V/dImw1pWvEGi3przWvbPmvFrNwem5togE3woXXeAjUw7H1BTcXTWZOeF4zk81+cdow2WGUBlj2b1qDdoN7rHfwb3Jmu0JGI414Cctd4zf+Vze80zLxB0cFIc3IwxIZO/h/klXYR8oJdOxolY="
                )
            )

            onInteract {
                val player = it.player
                val type = it.type

                when (type) {
                    NpcInteractionType.RIGHT_CLICK -> {
                        geckoAsyncScope.launch {
                            if (GeckoGameManager.selectGame(player) != null) {
                                player.sendText {
                                    appendPrefix()
                                    geckoPrimary("Du wurdest einer Runde zugewiesen.")
                                }
                            } else {
                                player.sendText {
                                    appendPrefix()
                                    geckoPrimary("Du konntest keiner Runde zugewiesen werden.")
                                }
                            }
                        }
                    }

                    NpcInteractionType.LEFT_CLICK -> {
                        geckoGamesView.open(player)
                    }
                }
            }
        }
    }
}