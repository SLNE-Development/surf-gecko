package dev.slne.surf.gecko.server.config

import net.minestom.server.Auth
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class Config(
    @Setting("address")
    val address: AddressConfig = AddressConfig(),

    @Setting("velocity")
    val velocity: VelocityConfig = VelocityConfig(),

    @Setting("performance")
    val performance: PerformanceConfig = PerformanceConfig(),

    @Setting("chat")
    val chat: ChatConfig = ChatConfig(),

    @Setting("max-players")
    @Comment("Player limit reported on the server list and enforced by the extensions.")
    val maxPlayers: Int = 100,
) {

    @ConfigSerializable
    data class AddressConfig(
        @Comment("Address on which the server listens.")
        val host: String = "0.0.0.0",

        @Comment("Port on which the server listens.")
        val port: Int = 25565,
    )

    @ConfigSerializable
    data class VelocityConfig(
        val enabled: Boolean = false,
        val secret: String = "secret"
    ) {
        fun createAuth(): Auth = if (enabled) {
            Auth.Velocity(secret)
        } else {
            Auth.Online()
        }
    }

    @ConfigSerializable
    data class DatabasePoolConfig(
        @Setting("maximum-size")
        @Comment("Maximum number of physical database connections.")
        val maximumSize: Int = 10,

        @Setting("minimum-idle")
        val minimumIdle: Int = 1,

        @Setting("connection-timeout-millis")
        val connectionTimeoutMillis: Long = 10_000,

        @Setting("validation-timeout-millis")
        val validationTimeoutMillis: Long = 5_000,
    )

    @ConfigSerializable
    data class ChatConfig(
        @Setting("enforce-secure-profile")
        @Comment(
            """
            Whether clients must present a signed chat session before they may chat.
            When enabled, players without a Mojang-issued profile key (offline mode,
            some proxies, Geyser) are told chat is disabled instead of being allowed
            to send unsigned messages.
            """
        )
        val enforceSecureProfile: Boolean = false,

        @Setting("chat-spam-threshold-seconds")
        @Comment(
            """
            Vanilla's chat-spam-threshold-seconds. Each chat message adds 20 to a
            counter that drains by 1 per tick; exceeding 20 * this value disconnects
            the player with "disconnect.spam".
            """
        )
        val chatSpamThresholdSeconds: Int = 10,

        @Setting("command-spam-threshold-seconds")
        @Comment("Vanilla's command-spam-threshold-seconds. Same mechanism as above, for commands.")
        val commandSpamThresholdSeconds: Int = 10,
    )

    @ConfigSerializable
    data class PerformanceConfig(
        @Setting("tick-threads")
        @Comment(
            """
            Number of threads used to tick the world (chunks & entities).
            Set to 0 to use the number of available CPU cores.
            For large player counts a good starting point is the number of
            physical cores.
            An explicit -Dminestom.dispatcher-threads=<n> JVM flag overrides this.
            """
        )
        val tickThreads: Int = 1,

        @Setting("spark")
        val spark: SparkConfig = SparkConfig(),
    )

    @ConfigSerializable
    data class SparkConfig(
        @Setting("profile-on-startup")
        @Comment(
            """
            Whether Spark should start profiling every thread as soon as the server boots.
            Useful while tuning a server, but it keeps a sampler running for the whole uptime.
            """
        )
        val profileOnStartup: Boolean = true,
    )
}
