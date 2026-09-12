package dev.slne.surf.gecko.server.gecko.display.tablist

import net.kyori.adventure.text.Component

data class TabEntry(
    val displayName: Component,
    val listOrder: Int
)