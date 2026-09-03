plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
    // Zipline fork gradle plugin (io.github.shabinder:zipline-gradle-plugin @ 1.27.2-CUSTOM-2+).
    // Central's 1.27.2-CUSTOM is a stale pre-Kotlin-2.4 build. Filtered so only zipline requests
    // hit this repo (GH Packages 401s unauthenticated; any GitHub token with read:packages works).
    maven {
        name = "GitHubPackagesZipline"
        url = uri("https://maven.pkg.github.com/Shabinder/zipline")
        credentials {
            username = System.getenv("GH_PACKAGES_USER") ?: System.getenv("GITHUB_ACTOR") ?: "Shabinder"
            password = System.getenv("GH_PACKAGES_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: ""
        }
        content { includeModuleByRegex("io\\.github\\.shabinder", "zipline.*") }
    }
}

dependencies {
    with(deps) {
        implementation(zipline.gradlePlugin)
        implementation(androidx.gradle.plugin)
        implementation(mavenPublish.gradle.plugin)
        with(kotlin) {
            implementation(kotlinGradlePlugin)
            implementation(serialization)
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}
