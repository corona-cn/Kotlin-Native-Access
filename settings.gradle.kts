/* === ROOT PROJECT CONFIGURATION === */
// Configure root project name
rootProject.name = "KNA"


/* === PLUGIN MANAGEMENT CONFIGURATION === */
pluginManagement {
    // Define Kotlin version for all plugins
    val kotlinVersion = "2.3.20-Beta1"

    // Configure plugin versions
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
    }

    // Configure plugin resolution strategy
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion(kotlinVersion)
            }
        }
    }
}


/* === PROJECT INCLUSION CONFIGURATION === */
// Include all subprojects in the build
include("kna-core")