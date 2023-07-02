plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("kotlin-parcelize")
    id("publish")
}

group = "in.shabinder"
version = (deps.soundbound.extensions.lib.get().version as String).also {
    println("Building with lib version: $it")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "in.shabinder.soundbound.extensions"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

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

kotlin {
    ios()
    android {
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
        nodejs {
            testTask { useMocha { timeout = "30000" } }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                with(deps) {
                    api(bundles.kotlinx)
                    api(essenty.parcelable)
                    api(compose.runtime) // for @Stable, @Immutable, etc annotations
                    implementation(ktor.client.core)
                    api(paging.common)
                    api(fuzzy.wuzzy)
                }
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(deps.ktor.client.android)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(deps.ktor.client.cio)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(deps.ktor.client.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(deps.ktor.client.ios)
            }
        }
        val iosTest by getting
    }

    dependencies {
        coreLibraryDesugaring(deps.androidx.desugar)
    }
}
