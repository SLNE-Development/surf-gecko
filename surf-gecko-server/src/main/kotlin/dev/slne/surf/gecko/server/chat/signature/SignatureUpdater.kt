package dev.slne.surf.gecko.server.chat.signature


fun interface SignatureUpdater {
    fun update(output: Output)

    fun interface Output {
        fun update(payload: ByteArray)
    }
}
