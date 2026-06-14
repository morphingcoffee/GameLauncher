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
                implementation(project(":core:model"))
                implementation(project(":core:logging"))
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
                implementation(project(":core:network"))
                implementation(project(":feature:home"))
                implementation(project(":feature:logs"))
                implementation(project(":feature:settings"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.navigation3.ui)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
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

private fun composeDesktopHostId(): String {
    val validHosts =
        setOf(
            "macos-arm64",
            "macos-x64",
            "windows-x64",
            "linux-arm64",
            "linux-x64",
        )
    val override =
        (findProperty("composeDesktopHost") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    if (override != null) {
        require(override in validHosts) {
            "Invalid composeDesktopHost '$override'; expected one of $validHosts"
        }
        return override
    }

    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    return when {
        os.contains("Mac", ignoreCase = true) && arch == "aarch64" -> "macos-arm64"
        os.contains("Mac", ignoreCase = true) -> "macos-x64"
        os.contains("Win", ignoreCase = true) -> "windows-x64"
        os.contains("Linux", ignoreCase = true) && arch == "aarch64" -> "linux-arm64"
        os.contains("Linux", ignoreCase = true) -> "linux-x64"
        else -> error("Unsupported OS for Compose Desktop: $os ($arch)")
    }
}

private fun composeDesktopHostDependency(composeVersion: String): String {
    val hostId = composeDesktopHostId()
    return "org.jetbrains.compose.desktop:desktop-jvm-$hostId:$composeVersion"
}

/** Optional `-PbuildNumber=…` from CI (`github.run_number`) — shared across macOS and Windows packaging. */
private fun ciBuildNumberProperty(): String? =
    (findProperty("buildNumber") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** Marketing version plus CI build suffix for artifact filenames (e.g. `0.0.1-build42`). */
private fun artifactVersionLabel(): String {
    val marketing =
        compose.desktop.application.nativeDistributions.packageVersion
            ?: error("packageVersion is not set")
    val build = ciBuildNumberProperty()
    return if (build != null) "$marketing-build$build" else marketing
}

/** jpackage MSI product version — must increase monotonically for in-place upgrades. */
private fun jpackageWindowsMsiVersion(): String {
    val build = ciBuildNumberProperty()
    return if (build != null) "1.0.$build" else "1.0.0"
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
            licenseFile.set(layout.projectDirectory.file("installer-license.rtf"))

            val iconsDir = layout.projectDirectory.dir("icons")

            macOS {
                bundleID = "com.morphingcoffee.gamelauncher.desktop"
                // JDK 17 jpackage rejects app-version with major 0; keep global 0.0.1 for artifact names.
                packageVersion = "1.0.0"
                iconFile.set(iconsDir.file("icon.icns"))
                ciBuildNumberProperty()?.let { packageBuildVersion = it }
            }

            windows {
                menu = true
                shortcut = true
                menuGroup = "Game Launcher"
                // Stable upgrade code — never change after first public MSI release.
                upgradeUuid = "8f2a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b"
                iconFile.set(iconsDir.file("icon.ico"))
                // jpackage MSI product version (distinct from marketing packageVersion 0.0.1).
                packageVersion = "1.0.0"
                msiPackageVersion = jpackageWindowsMsiVersion()
            }
        }
    }
}

tasks.register("runDevDesktop") {
    group = "compose desktop"
    description = "Run desktop app with fake catalog data and simulated network delays"
    dependsOn("run")
}

gradle.taskGraph.whenReady {
    if (gradle.taskGraph.hasTask(":composeApp:runDevDesktop")) {
        tasks.named<org.gradle.api.tasks.JavaExec>("run").configure {
            jvmArgs("-Dgame.launcher.dev=true")
        }
    }
}

tasks.register("printPackageVersion") {
    notCompatibleWithConfigurationCache("Reads packageVersion from Compose Desktop DSL")
    group = "distribution"
    description = "Prints marketing packageVersion (no CI build suffix)"
    doLast {
        println(
            compose.desktop.application.nativeDistributions.packageVersion
                ?: error("packageVersion is not set"),
        )
    }
}

tasks.register("printArtifactVersion") {
    notCompatibleWithConfigurationCache("Reads packageVersion and optional -PbuildNumber")
    group = "distribution"
    description = "Prints artifact version label for CI filenames (e.g. 0.0.1-build42)"
    doLast {
        println(artifactVersionLabel())
    }
}

tasks.register("printComposeDesktopHost") {
    notCompatibleWithConfigurationCache("Reads composeDesktopHost from Gradle properties or JVM os.arch")
    group = "distribution"
    description = "Prints the Compose Desktop Skiko host id (macos-arm64, macos-x64, …)"
    doLast {
        println(composeDesktopHostId())
    }
}
