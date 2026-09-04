import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  id(deps.plugins.android.kmp.library.get().pluginId)
  id(deps.plugins.kotlin.multiplatform.get().pluginId)
  id(deps.plugins.kotlin.parcelize.get().pluginId)
  id(deps.plugins.kotlin.serialization.get().pluginId)
  id(deps.plugins.zipline.gradle.get().pluginId)
  id(deps.plugins.maven.publish.config.get().pluginId)
  id(deps.plugins.version.catalog.get().pluginId)
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

mavenPublishing {
  publishToMavenCentral(true)
  // Only sign when a GPG key is configured (Maven Central release). Local-dev publishing to
  // mavenLocal (for building extension Zipline binaries against the lib) has no key -> skip.
  // vanniktech's plugin signs with `signingInMemoryKey` (supplied as
  // ORG_GRADLE_PROJECT_signingInMemoryKey). Guard on the SAME property the plugin reads: the old
  // GPG_PRIVATE_KEY guard was a different name from the one that carries the key, so setting only
  // GPG_PRIVATE_KEY enabled signing the plugin had no key for, and setting only signingInMemoryKey
  // skipped signAllPublications() and produced UNSIGNED artifacts, which Maven Central rejects.
  // Matches the convention in references/Nucleus and references/ComposeNativeWebview.
  if (project.findProperty("signingInMemoryKey") != null) {
    signAllPublications()
  }

  pom {
    name.set("soundbound-extensions-lib")
    description.set("SoundBound Extensions Stub")
    url.set("https://github.com/Shabinder/soundbound-extensions-lib/")

    licenses {
      license {
        name.set("GPL-3.0 License")
        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
      }
    }
    developers {
      developer {
        id.set("shabinder")
        name.set("Shabinder Singh")
        email.set("dev.shabinder@gmail.com")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/Shabinder/soundbound-extensions-lib.git")
      developerConnection.set("scm:git:ssh://github.com/Shabinder/soundbound-extensions-lib.git")
      url.set("https://github.com/Shabinder/soundbound-extensions-lib/")
    }
    issueManagement {
      system.set("GitHub Issues")
      url.set("https://github.com/Shabinder/soundbound-extensions-lib/issues")
    }
  }
}

repositories {
  google()
  mavenLocal()
  mavenCentral()
  // Zipline fork artifacts (runtime libs + zipline-kotlin-plugin compiler classpath) at
  // 1.27.2-CUSTOM-2+ — Central's 1.27.2-CUSTOM is a stale pre-Kotlin-2.4 build. Filtered so only
  // zipline requests hit this repo (GH Packages needs any GitHub token with read:packages).
  maven("https://maven.pkg.github.com/Shabinder/zipline") {
    name = "GitHubPackagesZipline"
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

kotlin {
  iosArm64()
  iosSimulatorArm64()
  androidLibrary {
    namespace = "in.shabinder.soundbound.extensions"
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
          api(kotlinx.coroutines)
        }
      }
    }
  }
}

plugins.withType<YarnPlugin> {
  the<YarnRootExtension>().yarnLockAutoReplace = true
}
