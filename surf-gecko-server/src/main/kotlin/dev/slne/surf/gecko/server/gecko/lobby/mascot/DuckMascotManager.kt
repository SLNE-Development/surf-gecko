package dev.slne.surf.gecko.server.gecko.lobby.mascot

import dev.slne.surf.gecko.server.coroutine.geckoScope
import dev.slne.surf.gecko.server.coroutine.ticks
import dev.slne.surf.gecko.server.gecko.lobby.GeckoLobby
import dev.slne.surf.gecko.server.gecko.sound.GeckoSounds
import dev.slne.surf.gecko.server.i18n.sendTranslatedActionBar
import dev.slne.surf.gecko.server.i18n.translateLines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.entity.Player
import net.worldseed.multipart.ModelEngine
import net.worldseed.resourcepack.PackBuilder
import java.io.StringReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

object DuckMascotManager {
    private val talking = ConcurrentHashMap.newKeySet<UUID>()

    lateinit var mascot: DuckMascot

    @OptIn(ExperimentalPathApi::class)
    fun create() {
        val base = Path("wsee")
        val bbmodels = base.resolve("bbmodel").createDirectories()
        val resourcePack = base.resolve("resourcepack")
        val models = base.resolve("models")

        resourcePack.deleteRecursively()
        models.deleteRecursively()

        javaClass.getResourceAsStream("/wsee/duck_mascot_large_flying.bbmodel")!!.use { input ->
            bbmodels.resolve("duck_mascot_large_flying.bbmodel").outputStream()
                .use { input.copyTo(it) }
        }

        val config = PackBuilder.generate(bbmodels, resourcePack, models)
        ModelEngine.loadMappings(StringReader(config.modelMappings()), models)

        mascot = DuckMascot(GeckoLobby.instance, GeckoLobby.mascotPos)
    }

    fun talk(player: Player) {
        if (!talking.add(player.uuid)) {
            return
        }

        geckoScope.launch {
            player.playSound(GeckoSounds.DIALOG_OPEN, Sound.Emitter.self())
            mascot.play("nod")
            delay(0.7.seconds)

            val lines = player.translateLines("lobby.mascot.lines")
                .map { PlainTextComponentSerializer.plainText().serialize(it) }

            for ((index, line) in lines.withIndex()) {
                if (!player.isOnline || !GeckoLobby.contains(player)) {
                    return@launch
                }

                if (index > 0) {
                    player.playSound(GeckoSounds.DIALOG_NEXT, Sound.Emitter.self())
                }

                mascot.play("quack")

                for (length in 1..line.length) {
                    player.sendTranslatedActionBar("lobby.mascot.speech", "text" to line.take(length))

                    if (length % 2 == 0 && !line[length - 1].isWhitespace()) {
                        player.playSound(GeckoSounds.DIALOG_BLIP, Sound.Emitter.self())
                    }

                    delay(1.ticks)
                }

                delay(2.seconds)
            }

            player.sendActionBar(Component.empty())
            mascot.play("jump")
            delay(1.6.seconds)
        }.invokeOnCompletion { talking.remove(player.uuid) }
    }
}
