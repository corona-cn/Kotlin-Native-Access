/* === PLUGIN === */
plugins {
    kotlin("jvm")
    `maven-publish`
}


/* === CONFIGURATION === */
kotlin {
    sourceSets {
        all {
            dependencies {
                /* Kotlin & Kotlinx */
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
            }
        }
    }
}