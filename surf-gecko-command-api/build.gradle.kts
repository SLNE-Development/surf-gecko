plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
    maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    maven("https://repo.lucko.me/")
}

dependencies {
    // The command API itself lives in the lobby API - this module only supplies the Minestom
    // platform it compiles onto, so the surf-*-minestom plugins register against the same one.
    compileOnlyApi(libs.minestom.lobby.api)
    compileOnlyApi(libs.minestom)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.coroutines.core)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.luckperms.minestom)
}

kotlin {
    jvmToolchain(25)
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
