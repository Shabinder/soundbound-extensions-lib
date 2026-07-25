rootProject.name = "soundbound-extensions-lib"

// Explicit plugin repos: the zipline fork plugin (io.github.shabinder @ 1.27.2-CUSTOM-2+) is NOT
// on the plugin portal or Central — Central's same-named 1.27.2-CUSTOM is a stale pre-Kotlin-2.4
// build. Resolve marker + plugin jar from mavenLocal or GitHub Packages (filtered; GH Packages
// 401s unauthenticated — any GitHub token with read:packages works).
pluginManagement {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.github.com/Shabinder/zipline") {
            name = "GitHubPackagesZipline"
            credentials {
                username = System.getenv("GH_PACKAGES_USER") ?: System.getenv("GITHUB_ACTOR") ?: "Shabinder"
                password = System.getenv("GH_PACKAGES_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
            content {
                includeModuleByRegex("io\\.github\\.shabinder", "zipline.*")
                includeModule("io.github.shabinder", "io.github.shabinder.gradle.plugin")
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
    }
}
include(":compose")
//include(":parcelize")
