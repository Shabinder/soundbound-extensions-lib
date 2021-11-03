plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

object Plugins {
    const val AGP = "4.2.2"
    const val DOKKA = "1.5.31"
    const val KOTLIN = "1.5.31"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Plugins.KOTLIN}")
    implementation("com.android.tools.build:gradle:${Plugins.AGP}")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:${Plugins.DOKKA}")
    implementation(kotlin("serialization", version = Plugins.KOTLIN))
    implementation("org.jetbrains.dokka:dokka-core:${Plugins.DOKKA}")
}