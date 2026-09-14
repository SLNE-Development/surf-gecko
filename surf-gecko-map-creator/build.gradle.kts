plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

group = "dev.slne.surf.gecko.map.creator"
version = findProperty("version") as String

surfPaperPluginApi {
    mainClass("dev.slne.surf.gecko.map.creator.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.add("red")
}

dependencies {
    implementation("dev.slne.surf.gecko:surf-gecko-common:1.0.0-SNAPSHOT")
}
