package dev.slne.surf.gecko.map.creator

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.gecko.map.creator.command.geckoMapCommand
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        geckoMapCommand()
    }
}