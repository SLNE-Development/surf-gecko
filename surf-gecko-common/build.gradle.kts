plugins {
    id("dev.slne.surf.api.gradle.core")
}

group = "dev.slne.surf.gecko"
version = findProperty("version") as String

surfCoreApi {
    withCoreCommon()
    withSurfRedis()
}

tasks.shadowJar {
    enabled = false
}

publishing {
    repositories {
        maven("https://reposilite.slne.dev/releases/") {
            name = "slne-repository-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
