import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  id(deps.plugins.android.library.get().pluginId)
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
  signAllPublications()

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
}

android {
  namespace = "in.shabinder.soundbound.extensions"
  compileSdk = deps.versions.androidCompileSdk.get().toInt()

  defaultConfig {
    minSdk = deps.versions.androidMinSdk.get().toInt()
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

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
          api(kotlinx.coroutines)
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
