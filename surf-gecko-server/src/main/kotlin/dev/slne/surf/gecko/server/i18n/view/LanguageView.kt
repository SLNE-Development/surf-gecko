package dev.slne.surf.gecko.server.i18n.view

import dev.slne.surf.api.minestom.inventory.framework.modifyConfig
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIcon
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.minestom.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.onOpen
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.i18n.*
import kotlinx.coroutines.launch
import net.minestom.server.entity.Player

val languageView = surfView("Language") {
    settings {
        rows(3)
        cancelAllInteractions()
        navigateBackOnOutsideClick(false)
    }

    onOpen {
        val title = player.translate("language.menu.title")
        modifyConfig { title(title) }
    }

    onFirstRender {
        GeckoLanguage.entries.forEachIndexed { index, language ->
            slot(2, 4 + index * 2, languageItem(player, language)).onClick { click ->
                click.closeForPlayer()
                select(click.player, language)
            }
        }

        slot(3, 1, ViewIcon(ViewIconType.CROSS, ViewIconColor.RED).build {
            displayName(player.translate("common.menu.close"))
        }).onClick { click ->
            click.closeForPlayer()
        }
    }
}

private fun languageItem(viewer: Player, language: GeckoLanguage) =
    ViewIcon(
        if (viewer.language == language) ViewIconType.CHECK else ViewIconType.CIRCLE,
        if (viewer.language == language) ViewIconColor.GREEN else ViewIconColor.WHITE,
    ).build {
        displayName(GeckoTranslations.render(language, "language.name"))
        lore(
            *viewer.translateLines(
                if (viewer.language == language) "language.menu.selected" else "language.menu.select",
                "language" to GeckoTranslations.render(language, "language.name"),
            ).toTypedArray()
        )
    }

private fun select(player: Player, language: GeckoLanguage) {
    if (player.language == language) {
        player.sendTranslated("language.already-selected", "language" to GeckoTranslations.render(language, "language.name"))
        return
    }

    geckoAsyncScope.launch {
        runCatching { PlayerLanguages.set(player, language) }
            .onSuccess {
                player.sendTranslated(
                    "language.changed",
                    "language" to GeckoTranslations.render(language, "language.name")
                )
            }
            .onFailure { player.sendTranslated("language.save-failed") }
            .getOrThrow()
    }
}
