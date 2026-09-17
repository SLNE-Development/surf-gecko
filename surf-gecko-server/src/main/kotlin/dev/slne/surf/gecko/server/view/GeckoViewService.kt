package dev.slne.surf.gecko.server.view

import com.google.inject.Singleton
import dev.slne.surf.api.minestom.inventory.framework.register
import dev.slne.surf.gecko.server.gecko.lobby.view.geckoGamesView
import dev.slne.surf.gecko.server.gecko.shop.shopView
import dev.slne.surf.gecko.server.lifecycle.GeckoService

@Singleton
class GeckoViewService : GeckoService {
    override suspend fun start() {
        shopView.register()
        geckoGamesView.register()
    }
}
