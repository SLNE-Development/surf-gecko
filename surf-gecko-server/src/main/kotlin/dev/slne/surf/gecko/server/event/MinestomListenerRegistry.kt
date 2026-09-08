package dev.slne.surf.gecko.server.event

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

object MinestomListenerRegistry {
    fun register(
        node: EventNode<Event>,
        listener: MinestomListener
    ) {
        listener::class.declaredFunctions
            .filter { it.findAnnotation<EventHandler>() != null }
            .forEach { function ->
                val eventClass = function.parameters
                    .drop(1)
                    .firstOrNull { parameter ->
                        parameter.type.classifier is KClass<*> &&
                                Event::class.java.isAssignableFrom(
                                    (parameter.type.classifier as KClass<*>).java
                                )
                    }
                    ?.type
                    ?.classifier as? KClass<out Event>
                    ?: return@forEach


                node.addListener(eventClass.java) { event ->
                    function.call(listener, event)
                }
            }
    }

    fun create() {
        MinecraftServer.getGlobalEventHandler().addChild(geckoEventHandlerNode)
    }
}

private val geckoEventHandlerNode = EventNode.all("surf-gecko-event-handler")

fun MinestomListener.register(node: EventNode<Event> = geckoEventHandlerNode) {
    MinestomListenerRegistry.register(node, this)
}