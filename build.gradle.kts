/* === SERIES INFORMATION === */
val seriesId = "kna"
val seriesGroupId = "kna"
val seriesVersion = "1.0"


/* === OWNER INFORMATION === */
val ownerId = "corona-cn"
val ownerName = "CoronaCN"


/* === ALL PROJECT CONFIGURATION === */
allprojects {
    // Configure inheritance
    this@allprojects.group = seriesGroupId
    this@allprojects.version = seriesVersion

    // Configure repository
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}


/* === SUBPROJECT CONFIGURATION === */
subprojects {
    // Ensure jdk version
    val jdkVersion = 25

    // Configure java encoding and compatibility
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        targetCompatibility = jdkVersion.toString()
        sourceCompatibility = jdkVersion.toString()
    }

    // Configure java toolchain
    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(jdkVersion))
            }
        }
    }
}


/* === PUBLICATION CONFIGURATION === */
subprojects {
    val subprojectId = this@subprojects.name
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            // Configure publication repository
            repositories {
                mavenLocal()
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/$ownerId/$seriesId")
                    credentials {
                        username = project.findProperty("GITHUB_ACTOR") as String? ?: System.getenv("GITHUB_ACTOR") ?: throw GradleException("GITHUB_ACTOR not found")
                        password = project.findProperty("GITHUB_TOKEN") as String? ?: System.getenv("GITHUB_TOKEN") ?: throw GradleException("GITHUB_TOKEN not found")
                    }
                }
            }

            // Configure publication
            publications {
                // Configure all publication
                all {
                    // Configure artifactId for all subprojects
                    if (this is MavenPublication) {
                        artifactId = subprojectId
                    }
                }

                // Configure maven publication
                create<MavenPublication>("maven") {
                    // Configure basic information
                    groupId = seriesGroupId
                    artifactId = subprojectId
                    version = seriesVersion

                    // Configure pom
                    pom {
                        // Configure information
                        name.set(subprojectId)
                        description.set("Description of $subprojectId")
                        url.set("https://github.com/$ownerId/$seriesId/blob/$subprojectId")

                        // Configure license
                        licenses {
                            license {
                                name.set("License of $subprojectId")
                                url.set("https://github.com/$ownerId/$seriesId/blob/$subprojectId/LICENSE.txt")
                            }
                        }

                        // Configure developer
                        developers {
                            developer {
                                id.set(ownerId)
                                name.set(ownerName)
                            }
                        }
                    }
                }
            }
        }
    }
}