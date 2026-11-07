package dev.voidapp.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Wires up the application module for AGP 9.x.
 *
 * AGP 9 compiles Kotlin through its built-in Kotlin support, so no
 * kotlin-android plugin is applied here. The Kotlin version used for
 * compilation comes from AGP's runtime dependency on KGP; the root build
 * script raises that to the project's Kotlin version.
 */
class VoidAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            fun catalogVersion(key: String) = libs.findVersion(key).get().requiredVersion

            extensions.configure<ApplicationExtension> {
                namespace = "dev.voidapp"
                compileSdk = catalogVersion("android-compileSdk").toInt()

                defaultConfig {
                    applicationId = "dev.voidapp"
                    minSdk = catalogVersion("android-minSdk").toInt()
                    targetSdk = catalogVersion("android-targetSdk").toInt()
                    versionCode = catalogVersion("voidVersionCode").toInt()
                    versionName = catalogVersion("voidVersionName")
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                        // A release build must be installable straight off the GitHub
                        // Release page, so it is signed with the debug keystore by
                        // default. Drop in your own keystore (see README) to sign it
                        // with a private key instead.
                        signingConfig = signingConfigs.getByName("debug")
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                buildFeatures {
                    compose = true
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }
        }
    }
}
