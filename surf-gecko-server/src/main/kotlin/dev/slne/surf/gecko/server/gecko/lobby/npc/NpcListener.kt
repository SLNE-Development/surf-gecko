package dev.slne.surf.gecko.server.gecko.lobby.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.minestom.lobby.api.event.EventRegistrar
import jakarta.inject.Singleton
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode

@Singleton
class NpcListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        val npcNode = EventNode.type("npc", EventFilter.INSTANCE)
        node.addChild(npcNode)
        StomNPCs.initialize(npcNode)
    }
}