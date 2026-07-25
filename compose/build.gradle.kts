import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id(deps.plugins.android.library.get().pluginId)
  id(deps.plugins.kotlin.multiplatform.get().pluginId)
  id("maven-publish")
}

group = "io.github.shabinder"
version = "1.0.1"

kotlin {
  iosArm64()
  iosSimulatorArm64()
  androidTarget {
    publishLibraryVariants("release", "debug")
  }
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

android {
  namespace = "in.shabinder.soundbound.compose"
  compileSdk = deps.versions.androidCompileSdk.get().toInt()

  defaultConfig {
    minSdk = deps.versions.androidMinSdk.get().toInt()
  }

  //sourceSets["main"].manifest.srcFile("../src/androidMain/AndroidManifest.xml")

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }

  kotlin {
    jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
  }

  dependencies {
    coreLibraryDesugaring(deps.androidx.desugar)
  }
}
