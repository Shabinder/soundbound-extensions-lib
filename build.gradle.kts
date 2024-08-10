import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id(deps.plugins.android.library.get().pluginId)
    id(deps.plugins.kotlin.multiplatform.get().pluginId)
    id(deps.plugins.kotlin.parcelize.get().pluginId)
    id(deps.plugins.kotlin.serialization.get().pluginId)
    id(deps.plugins.zipline.gradle.get().pluginId)
    id("publish")
}

group = "io.github.shabinder"
version = (deps.soundbound.extensions.lib.get().version as String).also {
    println("Building with lib version: $it")
}

afterEvaluate {
    catalog {
        // declare the aliases, bundles and versions in this block
        versionCatalog {
            from(files("gradle/deps.versions.toml"))
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["versionCatalog"])
                artifactId = "soundbound-extensions-catalog"
            }
        }
    }
}


repositories {
    google()
    mavenLocal()
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
    //ios()
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
        browser { testTask(jsTestAction) }
        binaries.executable()
        nodejs { testTask(jsTestAction) }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":compose"))
                //api(project(":parcelize"))
                with(deps) {
                    api(zipline)
                    api(kotlinx.serialization.json)
                }
            }
        }
    }

    dependencies {
        coreLibraryDesugaring(deps.androidx.desugar)
    }
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().yarnLockAutoReplace = true
}