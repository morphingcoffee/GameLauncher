import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.morphingcoffee.gamelauncher.feature.logs"
        compileSdk {
            version =
                release(
                    libs.versions.android.compileSdk
                        .get()
                        .toInt(),
                ) {
                    minorApiLevel =
                        libs.versions.android.compileSdkMinor
                            .get()
                            .toInt()
                }
        }
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling.desktop)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":core:designsystem"))
                implementation(project(":core:logging"))
                implementation(project(":core:model"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
