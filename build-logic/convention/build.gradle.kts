import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.voidapp.buildlogic"

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
            id = "voidapp.android.application"
            implementationClass = "dev.voidapp.buildlogic.VoidAndroidApplicationPlugin"
        }
        register("androidCompose") {
            id = "voidapp.android.compose"
            implementationClass = "dev.voidapp.buildlogic.VoidAndroidComposePlugin"
        }
    }
}
