import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id(deps.plugins.android.kmp.library.get().pluginId)
  id(deps.plugins.kotlin.multiplatform.get().pluginId)
  id("maven-publish")
}

group = "io.github.shabinder"
version = "1.0.1"

kotlin {
  iosArm64()
  iosSimulatorArm64()
  androidLibrary {
    namespace = "in.shabinder.soundbound.compose"
    compileSdk = deps.versions.androidCompileSdk.get().toInt()
    minSdk = deps.versions.androidMinSdk.get().toInt()
    enableCoreLibraryDesugaring = true
  }
  jvmToolchain(21)
  jvm {
    testRuns["test"].executionTask.configure {
      useJUnit()
    }
  }
  js(IR) {
    browser {
      testTask { useMocha { timeout = "30000" } }
    }
    binaries.executable()
    nodejs {
      testTask { useMocha { timeout = "30000" } }
    }
  }
}

repositories {
  google()
  mavenLocal()
  mavenCentral()
  // Zipline fork artifacts at 1.27.2-CUSTOM-2+ (transitive via the lib project) — Central's
  // 1.27.2-CUSTOM is a stale pre-Kotlin-2.4 build. Filtered; needs any GitHub token with
  // read:packages when mavenLocal doesn't have the artifacts.
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
  coreLibraryDesugaring(deps.androidx.desugar)
}
