plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
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