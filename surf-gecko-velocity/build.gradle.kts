plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

surfVelocityApi {
    withCoreCommon()
    withSurfRedis()
}

group = "dev.slne.surf.gecko.velocity"

velocityPluginFile {
    main = "dev.slne.surf.gecko.velocity.VelocityMain"
    authors = listOf("red")

    pluginDependencies {
        register("surf-rabbitmq-velocity")
    }
}

dependencies {
    implementation(projects.surfGeckoCommon)
}
