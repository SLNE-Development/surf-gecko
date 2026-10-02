package dev.slne.surf.gecko.server.gecko.lobby.mascot

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.instance.Instance
import net.worldseed.multipart.animations.AnimationHandlerImpl

class DuckMascot(instance: Instance, pos: Pos) : Entity(EntityType.INTERACTION) {
    private val model = DuckMascotModel()
    private val animations: AnimationHandlerImpl

    init {
        setNoGravity(true)
        editEntityMeta(InteractionMeta::class.java) {
            it.width = 1.5f
            it.height = 2.8f
            it.response = true
        }

        model.owner = this
        model.init(instance, pos)

        animations = AnimationHandlerImpl(model)
        animations.playRepeat("idle")

        setInstance(instance, pos)
    }

    fun play(animation: String) = animations.playOnce(animation, 4) {}

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        model.addViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)
        model.removeViewer(player)
    }

    override fun remove() {
        animations.destroy()
        model.destroy()
        super.remove()
    }
}
