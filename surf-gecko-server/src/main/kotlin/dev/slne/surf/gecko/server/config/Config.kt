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

    @Setting("anticheat")
    val antiCheat: AntiCheatConfig = AntiCheatConfig(),

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

    @ConfigSerializable
    data class AntiCheatConfig(
        @Setting("enabled")
        @Comment("Whether the simulation based movement anticheat runs at all.")
        val enabled: Boolean = true,

        @Setting("max-offset")
        @Comment(
            """
            Maximum distance in blocks between the simulated and the reported position
            before a movement tick is considered impossible. Lower values detect more,
            higher values are more forgiving towards physics edge cases.
            """
        )
        val maxOffset: Double = 0.03,

        @Setting("fluid-offset")
        @Comment("Tolerance used while the player is inside water, where buoyancy is approximated.")
        val fluidOffset: Double = 0.08,

        @Setting("climb-offset")
        @Comment("Tolerance used while the player is on a ladder, vine or scaffolding.")
        val climbOffset: Double = 0.05,

        @Setting("excess-budget")
        @Comment(
            """
            Blocks of accumulated unexplained movement before speed is reported. Catches boosts
            that stay below the per tick tolerance; legitimate movement drains the budget again.
            """
        )
        val excessBudget: Double = 0.4,

        @Setting("mitigate")
        @Comment("Whether impossible movement is corrected by setting the player back.")
        val mitigate: Boolean = true,

        @Setting("mitigation-threshold")
        @Comment("Violation level a check has to reach before setbacks are applied.")
        val mitigationThreshold: Double = 5.0,

        @Setting("mitigation-cooldown-millis")
        @Comment("Minimum time between two setbacks of the same player.")
        val mitigationCooldownMillis: Long = 400,

        @Setting("max-desync-offset")
        @Comment(
            """
            Distance between the simulated and the reported position that counts as a gross
            desynchronisation even when the player did not gain speed or height. Only movement
            that is faster or higher than any possible input is reported below this value.
            """
        )
        val maxDesyncOffset: Double = 0.15,

        @Setting("alert-threshold")
        @Comment("Violation level a check has to reach before staff members are alerted.")
        val alertThreshold: Double = 10.0,

        @Setting("violation-decay")
        @Comment("Violation level removed from every check on each player tick.")
        val violationDecay: Double = 0.02,

        @Setting("alert-cooldown-millis")
        @Comment("Minimum time between two alerts of the same check for the same player.")
        val alertCooldownMillis: Long = 1_500,

        @Setting("max-reach")
        @Comment("Maximum distance in blocks from the eye position to the hitbox of an attacked entity.")
        val maxReach: Double = 3.1,

        @Setting("reach-lag-compensation-millis")
        @Comment("Extra time window of recorded target positions considered when validating a hit.")
        val reachLagCompensationMillis: Long = 200,

        @Setting("max-timer-drift-millis")
        @Comment("Accumulated time the client may tick ahead of the server before the timer check flags.")
        val maxTimerDriftMillis: Long = 120,

        @Setting("ground-spoof-ticks")
        @Comment("Consecutive ticks a player may claim to stand on the ground without support.")
        val groundSpoofTicks: Int = 5,

        @Setting("phase-ticks")
        @Comment("Consecutive ticks a player may stay inside a solid block.")
        val phaseTicks: Int = 3,

        @Setting("join-grace-ticks")
        @Comment("Ticks after spawning during which movement is not validated.")
        val joinGraceTicks: Int = 60,

        @Setting("teleport-grace-ticks")
        @Comment("Ticks after a teleport during which movement is not validated.")
        val teleportGraceTicks: Int = 10,

        @Setting("knockback-grace-ticks")
        @Comment("Ticks after applied velocity during which movement is not validated.")
        val knockbackGraceTicks: Int = 15,
    )
}
