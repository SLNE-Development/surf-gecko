package dev.slne.surf.gecko.server.i18n

import dev.slne.surf.gecko.server.database.repository.GeckoPlayerLanguageRepository
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object PlayerLanguages {
    private val logger = ComponentLogger.logger("PlayerLanguages")
    private val languages = ConcurrentHashMap<UUID, GeckoLanguage>()
    private val listeners = CopyOnWriteArrayList<(Player) -> Unit>()

    fun get(player: Player): GeckoLanguage =
        languages[player.uuid] ?: GeckoLanguage.fromLocale(player.locale)

    suspend fun load(player: Player) {
        val language = runCatching {
            GeckoPlayerLanguageRepository.fetchLanguage(player.uuid)
                ?: GeckoLanguage.fromLocale(player.locale).also {
                    GeckoPlayerLanguageRepository.saveLanguage(player.uuid, it)
                }
        }.onFailure {
            logger.error("Could not load the language of {}", player.username, it)
        }.getOrElse { GeckoLanguage.fromLocale(player.locale) }

        languages[player.uuid] = language
    }

    fun onChange(listener: (Player) -> Unit) {
        listeners += listener
    }

    suspend fun set(player: Player, language: GeckoLanguage) {
        languages[player.uuid] = language
        listeners.forEach { listener ->
            runCatching { listener(player) }
                .onFailure { logger.error("Language change listener failed for {}", player.username, it) }
        }
        GeckoPlayerLanguageRepository.saveLanguage(player.uuid, language)
    }

    fun invalidate(uuid: UUID) {
        languages.remove(uuid)
    }
}
