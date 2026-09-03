package dev.slne.surf.gecko.server.player.config

object ResourcePackTask : ConfigurationTask {

    @Suppress("UnstableApiUsage")
    override fun run(context: ConfigurationContext) {
        context.player.resourcePackFuture?.join()
    }
}
