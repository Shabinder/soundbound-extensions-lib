@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

/*
 *  Copyright (c)  2021  Shabinder Singh
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("version-catalog")
    id("maven-publish")
    id("signing")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "maven"
                val freshRepoID = "SONATYPE_REPOSITORY_ID".byProperty
                val manualRepoID = "MANUAL_REPOSITORY".byProperty
                val repositoryId = if(freshRepoID.isNullOrBlank()) manualRepoID else freshRepoID
                logger.log(LogLevel.WARN,"-$repositoryId-")
                setUrl{
                    "https://s01.oss.sonatype.org/service/local/staging/deployByRepositoryId/${repositoryId}/"
                }
                // url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = "SONATYPE_USERNAME".byProperty ?: "NEXUS_ACTIONS_SONATYPE_USERNAME".byProperty
                    password = "SONATYPE_PASSWORD".byProperty ?: "NEXUS_ACTIONS_SONATYPE_PASSWORD".byProperty
                }
            }
            maven {
                name = "snapshot"
                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                credentials {
                    username = "SONATYPE_USERNAME".byProperty ?: "NEXUS_ACTIONS_SONATYPE_USERNAME".byProperty
                    password = "SONATYPE_PASSWORD".byProperty ?: "NEXUS_ACTIONS_SONATYPE_PASSWORD".byProperty
                }
            }
        }
        val javadocJar by tasks.creating(Jar::class) {
            archiveClassifier.value("javadoc")
            // TODO: instead of a single empty Javadoc JAR, generate real documentation for each module
        }

        publications {
            withType<MavenPublication> {
                artifact(javadocJar)

                pom {
                    if (!"USE_SNAPSHOT".byProperty.isNullOrBlank()) {
                        version = "$version-SNAPSHOT"
                    }
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
        }

        val signingKey = "GPG_PRIVATE_KEY".byProperty ?: "GPG_FILE".byProperty?.let { file(it).readText() }
        val signingPwd = "GPG_PRIVATE_PASSWORD".byProperty
        if (signingKey.isNullOrBlank() || signingPwd.isNullOrBlank()) {
            logger.info("Signing Disable as the PGP key was not found")
        } else {
            signing {
                useInMemoryPgpKeys(signingKey, signingPwd)
                sign(publishing.publications)
                sign(configurations.archives.get())
            }
        }
    }
}

val String.byProperty: String? get() = gradleLocalProperties(rootDir, providers).getProperty(this) ?: project.findProperty(this) as? String ?: System.getenv(this)
