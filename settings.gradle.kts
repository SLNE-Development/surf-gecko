enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://reposilite.slne.dev/releases")
    }

    plugins {
        kotlin("kapt") version "2.4.10"
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://repo.lucko.me/")
        maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
        maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    }
}

include("surf-gecko-server")