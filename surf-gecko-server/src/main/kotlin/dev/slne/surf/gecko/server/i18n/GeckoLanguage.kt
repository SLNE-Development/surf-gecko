package dev.slne.surf.gecko.server.i18n

import java.util.Locale

enum class GeckoLanguage(val id: String, val locale: Locale) {
    DE_DE("de_de", Locale.GERMANY),
    EN_US("en_us", Locale.US);

    companion object {
        val FALLBACK = DE_DE

        fun fromId(id: String): GeckoLanguage? = entries.firstOrNull { it.id.equals(id, true) }

        fun fromLocale(locale: Locale?): GeckoLanguage = when (locale?.language) {
            null, "" -> FALLBACK
            "de" -> DE_DE
            else -> EN_US
        }
    }
}
