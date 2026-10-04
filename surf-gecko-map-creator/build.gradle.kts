plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

group = "dev.slne.surf.gecko.map.creator"

surfPaperPluginApi {
    mainClass("dev.slne.surf.gecko.map.creator.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.add("red")
}

dependencies {
    implementation(projects.surfGeckoCommon)
}
