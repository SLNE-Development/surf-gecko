pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://reposilite.slne.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "2.2.1-PR435"
}

rootProject.name = "surf-gecko"

include("surf-gecko-common")
include("surf-gecko-server")
include("surf-gecko-velocity")
include("surf-gecko-map-creator")
