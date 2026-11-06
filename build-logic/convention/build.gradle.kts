import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.void.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "void.android.application"
            implementationClass = "dev.void.buildlogic.VoidAndroidApplicationPlugin"
        }
        register("androidCompose") {
            id = "void.android.compose"
            implementationClass = "dev.void.buildlogic.VoidAndroidComposePlugin"
        }
    }
}
