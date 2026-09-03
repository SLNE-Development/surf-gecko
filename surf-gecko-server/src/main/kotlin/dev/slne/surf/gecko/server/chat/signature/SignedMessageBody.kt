package dev.slne.surf.gecko.server.chat.signature

import dev.slne.surf.gecko.server.util.toByteArray
import java.time.Instant
import net.minestom.server.crypto.SignedMessageBody as NetworkSignedMessageBody

data class SignedMessageBody(
    val content: String,
    val timeStamp: Instant,
    val salt: Long,
    val lastSeen: LastSeenMessages
) {

    companion object {
        fun unsigned(content: String) =
            SignedMessageBody(content, Instant.now(), 0L, LastSeenMessages.EMPTY)
    }

    fun updateSignature(output: SignatureUpdater.Output) {
        output.update(salt.toByteArray())
        output.update(timeStamp.epochSecond.toByteArray())

        val contentBytes = content.toByteArray()
        output.update(contentBytes.size.toByteArray())
        output.update(contentBytes)

        lastSeen.updateSignature(output)
    }

    fun pack(cache: MessageSignatureCache): NetworkSignedMessageBody.Packed =
        NetworkSignedMessageBody.Packed(content, timeStamp, salt, lastSeen.pack(cache))
}
