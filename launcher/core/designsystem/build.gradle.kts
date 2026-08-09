import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.morphingcoffee.gamelauncher.core.designsystem"
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
        val desktopMain by getting {
            dependencies {
                implementation(libs.compose.ui.tooling.desktop)
                // Host-specific Skiko natives for RuntimeEffect / RuntimeShaderBuilder.
                implementation(
                    composeDesktopHostDependency(
                        libs.versions.compose.multiplatform
                            .get(),
                    ),
                )
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:logging"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

private fun composeDesktopHostId(): String {
    val override =
        (findProperty("composeDesktopHost") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    if (override != null) return override

    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.contains("mac") || os.contains("darwin") ->
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                "macos-arm64"
            } else {
                "macos-x64"
            }
        os.contains("win") ->
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                "windows-arm64"
            } else {
                "windows-x64"
            }
        os.contains("linux") ->
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                "linux-arm64"
            } else {
                "linux-x64"
            }
        else -> error("Unsupported OS for Compose Desktop: $os ($arch)")
    }
}

private fun composeDesktopHostDependency(composeVersion: String): String {
    val hostId = composeDesktopHostId()
    return "org.jetbrains.compose.desktop:desktop-jvm-$hostId:$composeVersion"
}
