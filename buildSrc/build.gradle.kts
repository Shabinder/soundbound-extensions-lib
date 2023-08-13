plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    with(deps) {
        implementation(zipline.gradlePlugin)
        implementation(androidx.gradle.plugin)
        with(kotlin) {
            implementation(kotlinGradlePlugin)
            implementation(serialization)
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}