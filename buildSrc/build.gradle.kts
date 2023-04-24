plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

object Plugins {
    const val AGP = "7.4.2"
    const val KOTLIN = "1.8.20"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Plugins.KOTLIN}")
    implementation("com.android.tools.build:gradle:${Plugins.AGP}")
    implementation(kotlin("serialization", version = Plugins.KOTLIN))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}