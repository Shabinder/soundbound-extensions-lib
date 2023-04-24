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
    implementation(deps.kotlin.kotlinGradlePlugin)
    implementation(deps.androidx.gradle.plugin)
    implementation(deps.kotlin.serialization)
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}