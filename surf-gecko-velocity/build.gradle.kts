plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

surfVelocityApi {
    withSurfRedis()
}

group = "dev.slne.surf.gecko.map.creator"
version = findProperty("version") as String

velocityPluginFile {
    main = "dev.slne.surf.core.velocity.VelocityMain"
    authors = listOf("red")

    pluginDependencies {
        register("surf-rabbitmq-velocity")
    }
}
