package dev.slne.surf.gecko.server.i18n.command

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.surf.gecko.server.coroutine.geckoAsyncScope
import dev.slne.surf.gecko.server.i18n.GeckoLanguage
import dev.slne.surf.gecko.server.i18n.GeckoTranslations
import dev.slne.surf.gecko.server.i18n.sendTranslated
import dev.slne.surf.gecko.server.permission.PermissionList
import kotlinx.coroutines.launch

fun i18nCommand() = commandTree("i18n") {
    withPermission(PermissionList.COMMAND_I18N)

    literalArgument("reload") {
        anyExecutor { sender, _ ->
            sender.sendTranslated("i18n.reload.started")

            geckoAsyncScope.launch {
                runCatching { GeckoTranslations.reload() }
                    .onSuccess { result ->
                        sender.sendTranslated(
                            "i18n.reload.success",
                            "source" to result.source.name.lowercase(),
                            "languages" to GeckoLanguage.entries.joinToString(", ") {
                                "${it.id} (${GeckoTranslations.keyCount(it)})"
                            },
                        )

                        for (language in GeckoLanguage.entries) {
                            val missing = GeckoTranslations.missingKeys(language)
                            if (missing.isEmpty()) continue

                            sender.sendTranslated(
                                "i18n.reload.missing",
                                "language" to language.id,
                                "count" to missing.size,
                                "keys" to missing.take(10).joinToString(", "),
                            )
                        }
                    }
                    .onFailure { sender.sendTranslated("i18n.reload.failed", "error" to it.toString()) }
                    .getOrThrow()
            }
        }
    }
}
