// Root build script.
//
// Every plugin used by any module is declared here once, so the build classpath
// is identical across projects.
//
// AGP 9 compiles Kotlin itself ("built-in Kotlin") and pins that compiler to the
// KGP version it depends on (2.2.10). Pulling the project's KGP onto the build
// classpath - without applying it anywhere - raises the built-in Kotlin compiler
// to the Kotlin version from gradle/libs.versions.toml, which is what the
// Compose compiler plugin (kotlin-compose) requires.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.gradlePlugin) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
