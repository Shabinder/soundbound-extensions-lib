plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("publish")
}

group = "in.shabinder"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}