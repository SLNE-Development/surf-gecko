package dev.slne.surf.gecko.command.internal.extension

import net.minestom.server.MinecraftServer.getCommandManager
import net.minestom.server.command.CommandManager
import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode

inline val CommandManager: CommandManager get() = getCommandManager()

inline fun <reified T : Event> EventNode<Event>.addListener(
    noinline listener: (T) -> Unit
) {
    addListener(EventListener.of(T::class.java, listener))
}
