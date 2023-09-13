import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("kotlin-parcelize")
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
        val jsTestAction = Action<KotlinJsTest> {
            useMocha { timeout = "30000" }
        }
        browser {
            testTask(jsTestAction)
        }
        binaries.executable()
        nodejs {
            testTask(jsTestAction)
        }
    }

    sourceSets {
        val commonMain by getting {}
        val notParcelableMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(notParcelableMain)
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
    namespace = "in.shabinder.soundbound.parcelize"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    // sourceSets["main"].manifest.srcFile("../src/androidMain/AndroidManifest.xml")

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