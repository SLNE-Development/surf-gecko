package dev.slne.surf.gecko.command.internal

import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.UUID

/** The profile id a message that no player sent is attributed to. */
internal val NIL_UUID: UUID = UUID(0L, 0L)

/**
 * A [SignedMessage] carrying no signature, for a sender that produced none: the console, or a
 * client that sent the command through a path that does not sign its arguments.
 *
 * A host with signed chat does not go through this - it feeds the signatures its chat handler
 * received into
 * [SignedCommandArguments.withMessages][dev.slne.surf.gecko.command.platform.SignedCommandArguments.withMessages]
 * before dispatching, and a signed message argument then reads the real message from there.
 */
internal class UnsignedMessage(
    private val senderId: UUID,
    private val content: String,
    private val sentAt: Instant = Instant.now(),
) : SignedMessage {

    override fun timestamp(): Instant = sentAt

    override fun salt(): Long = 0L

    override fun signature(): SignedMessage.Signature? = null

    override fun unsignedContent(): Component? = null

    override fun message(): String = content

    override fun identity(): Identity = Identity.identity(senderId)

    override fun toString(): String = "UnsignedMessage(sender=$senderId, content='$content')"
}
