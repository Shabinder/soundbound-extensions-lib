plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

object Plugins {
    const val AGP = "7.4.2"
    const val KOTLIN = "1.8.20"
}

dependencies {
    with(deps) {
        implementation(androidx.gradle.plugin)
        with(kotlin) {
            implementation(kotlinGradlePlugin)
            implementation(compose.gradle)
            implementation(serialization)
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}