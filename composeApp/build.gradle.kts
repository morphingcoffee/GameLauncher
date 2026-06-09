import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koinCompiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":core:architecture"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:navigation"))
                implementation(project(":feature:home"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
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

koinCompiler {
    compileSafety = true
}

compose.desktop {
    application {
        mainClass = "com.gamelauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "GameLauncher"
            packageVersion = "1.0.0"
            description = "Cross-platform desktop game launcher"
            vendor = "GameLauncher"

            macOS {
                bundleID = "com.gamelauncher.desktop"
            }
        }
    }
}
