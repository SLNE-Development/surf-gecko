plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "dev.slne.surf.gecko"
    version = "1.0.0-SNAPSHOT"
}