package dev.voidapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Compose setup for a module: applies the Compose compiler plugin and adds the
 * Compose BOM plus the UI/foundation/material3 dependencies every screen needs.
 */
class VoidAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            fun library(alias: String) = libs.findLibrary(alias).get()

            dependencies {
                val bom = platform(library("compose-bom"))
                add("implementation", bom)

                add("implementation", library("compose-ui"))
                add("implementation", library("compose-ui-graphics"))
                add("implementation", library("compose-ui-tooling-preview"))
                add("implementation", library("compose-foundation"))
                add("implementation", library("compose-material3"))
                add("debugImplementation", library("compose-ui-tooling"))

                add("implementation", library("androidx-activity-compose"))
                add("implementation", library("androidx-lifecycle-runtime"))
                add("implementation", library("androidx-webkit"))
                add("implementation", library("androidx-splashscreen"))
                add("implementation", library("kotlinx-coroutines-android"))

                // Remote images: shortcut favicons and the custom home background.
                add("implementation", library("coil-compose"))
                add("implementation", library("coil-network"))
            }
        }
    }
}
