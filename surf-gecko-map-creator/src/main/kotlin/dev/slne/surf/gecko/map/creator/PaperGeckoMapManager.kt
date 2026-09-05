package dev.slne.surf.gecko.map.creator

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.gecko.map.creator.draft.GeckoMapDraft
import dev.slne.surf.gecko.map.creator.item.MarkerItems
import dev.slne.surf.gecko.map.creator.render.MarkerRenderer
import org.bukkit.entity.Player
import java.util.*

object PaperGeckoMapManager {
    private val drafts = mutableObject2ObjectMapOf<UUID, GeckoMapDraft>()

    fun draft(playerUuid: UUID): GeckoMapDraft? = drafts[playerUuid]

    fun create(player: Player, mapName: String, mapDisplayName: String): GeckoMapDraft {
        val draft = GeckoMapDraft(
            ownerUuid = player.uniqueId,
            mapName = mapName,
            mapDisplayName = mapDisplayName,
            worldName = player.world.name,
        )
        draft.authors.add(GeckoMapDraft.DraftAuthor(player.name, player.uniqueId))
        drafts[player.uniqueId] = draft

        MarkerItems.giveTools(player)
        MarkerRenderer.enable(player)

        return draft
    }

    fun reset(player: Player): Boolean {
        val removed = drafts.remove(player.uniqueId) != null

        MarkerRenderer.disable(player)
        MarkerItems.clearTools(player)

        return removed
    }
}
