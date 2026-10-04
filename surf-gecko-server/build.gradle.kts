plugins {
    id("dev.slne.surf.api.gradle.minestom-server")
}

repositories {
    maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    maven("https://reposilite.atlasengine.ca/public") { name = "atlasengine-repository-public" }
}

surfMinestomServerApi {
    mainClass("dev.slne.surf.gecko.server.MainKt")

    withSignedChat()
    withLuckPerms()
    withSpark()
    withNpcLib()
    withConsole()
    withPlugins()
}

dependencies {
    implementation(projects.surfGeckoCommon)

    implementation(libs.surf.core.api.common)
    implementation(libs.surf.playtime.api.common)
    implementation(libs.surf.bitmap.provider.common)
    implementation(libs.surf.redis.api) {
        artifact { classifier = "all" }
    }
    implementation(libs.surf.database.r2dbc) {
        artifact { classifier = "all" }
    }

    implementation(libs.adventure.text.minimessage)
    implementation(libs.polar)
    implementation(libs.wsee)
    compileOnly(libs.flare.fastutil)

    implementation(libs.fabric.mixin)
    annotationProcessor(libs.fabric.mixin)
    implementation(libs.mixin.extra)
    annotationProcessor(libs.mixin.extra)
}

val bundleTranslations by tasks.registering {
    val source = rootProject.layout.projectDirectory.dir("surf-gecko-translations/lang").asFile
    val output = layout.buildDirectory.dir("generated/translations").get().asFile
    inputs.dir(source)
    outputs.dir(output)

    doLast {
        val target = output.resolve("lang")
        target.deleteRecursively()
        source.copyRecursively(target)

        val index = target.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .map { it.relativeTo(target).invariantSeparatorsPath }
            .sorted()
            .joinToString("\n")
        target.resolve("index.txt").writeText(index)
    }
}

sourceSets.main {
    resources.srcDir(bundleTranslations)
}

tasks.jar {
    archiveClassifier = "thin"
}

afterEvaluate {
    tasks.shadowJar {
        archiveClassifier = ""
        isZip64 = true

        manifest {
            attributes["Launcher-Agent-Class"] = "dev.slne.surf.gecko.server.instrumentation.GeckoRunner"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"

            attributes(
                mapOf("Implementation-Version" to libs.versions.asm.get()),
                "org/objectweb/asm/"
            )
        }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

apply(from = rootProject.file("gradle/deploy-dev-gecko.gradle.kts"))
