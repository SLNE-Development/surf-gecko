plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

surfVelocityApi {
    withCoreCommon()
    withSurfRedis()
}

group = "dev.slne.surf.gecko.velocity"
version = findProperty("version") as String

velocityPluginFile {
    main = "dev.slne.surf.gecko.velocity.VelocityMain"
    authors = listOf("red")

    pluginDependencies {
        register("surf-rabbitmq-velocity")
    }
}

dependencies {
    implementation("dev.slne.surf.gecko:surf-gecko-common:1.0.0-SNAPSHOT")
}
