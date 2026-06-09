import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koinCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop")

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val desktopMain by getting {
            dependencies {
                // Generic :desktop lacks Skiko natives; use the host OS/arch artifact (was compose.desktop.currentOs).
                implementation(
                    composeDesktopHostDependency(
                        libs.versions.compose.multiplatform
                            .get(),
                    ),
                )
                implementation(libs.compose.ui.tooling.desktop)
                // Main dispatcher for collectAsStateWithLifecycle / lifecycle-runtime-compose on Swing.
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":core:architecture"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:navigation"))
                implementation(project(":feature:home"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.navigation3.ui)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
            }
        }
    }
}

android {
    namespace = "com.morphingcoffee.gamelauncher"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

koinCompiler {
    // Cross-module ViewModels are not visible to compileSafety yet (Koin #2404).
    compileSafety = false
}

private fun composeDesktopHostDependency(composeVersion: String): String {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val hostId =
        when {
            os.contains("Mac", ignoreCase = true) && arch == "aarch64" -> "macos-arm64"
            os.contains("Mac", ignoreCase = true) -> "macos-x64"
            os.contains("Win", ignoreCase = true) -> "windows-x64"
            os.contains("Linux", ignoreCase = true) && arch == "aarch64" -> "linux-arm64"
            os.contains("Linux", ignoreCase = true) -> "linux-x64"
            else -> error("Unsupported OS for Compose Desktop: $os ($arch)")
        }
    return "org.jetbrains.compose.desktop:desktop-jvm-$hostId:$composeVersion"
}

compose.desktop {
    application {
        mainClass = "com.morphingcoffee.gamelauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "GameLauncher"
            packageVersion = "0.0.1"
            description = "Cross-platform desktop game launcher"
            vendor = "GameLauncher"

            macOS {
                bundleID = "com.morphingcoffee.gamelauncher.desktop"
                // JDK 17 jpackage rejects app-version with major 0; keep global 0.0.1 for artifact names.
                packageVersion = "1.0.0"
            }
        }
    }
}

tasks.register("printPackageVersion") {
    notCompatibleWithConfigurationCache("Reads packageVersion from Compose Desktop DSL")
    group = "distribution"
    description = "Prints packageVersion for CI artifact naming"
    doLast {
        println(
            compose.desktop.application.nativeDistributions.packageVersion
                ?: error("packageVersion is not set"),
        )
    }
}
