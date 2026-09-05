package dev.slne.surf.gecko.server.view

import com.google.inject.Singleton
import dev.slne.surf.api.minestom.inventory.framework.register
import dev.slne.surf.gecko.server.gecko.shop.shopView
import dev.slne.surf.gecko.server.lifecycle.GeckoService

/**
 * Registers the server's own views.
 *
 * This has to run before the extensions are started: the inventory framework only initializes the
 * views that are known to it when `surf-api-minestom` registers its view frame in `afterStart`,
 * and an uninitialized view cannot be opened.
 */
@Singleton
class GeckoViewService : GeckoService {
    override suspend fun start() {
        shopView.register()
    }
}
