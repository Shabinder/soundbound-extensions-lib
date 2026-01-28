import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(deps.plugins.android.library.get().pluginId)
    id(deps.plugins.kotlin.multiplatform.get().pluginId)
//    id("publish")
}

group = "io.github.shabinder"
version = "1.0.0"

kotlin {
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

    dependencies {
        coreLibraryDesugaring(deps.androidx.desugar)
    }
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
}

android {
    namespace = "in.shabinder.soundbound.compose"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
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
}
