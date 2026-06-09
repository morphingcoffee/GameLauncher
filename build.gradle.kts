import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.koinCompiler) apply false
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val composeVersion = versionCatalog.findVersion("compose-multiplatform").get().requiredVersion

subprojects {
    val composeAlignedGroups = setOf(
        "org.jetbrains.compose.runtime",
        "org.jetbrains.compose.foundation",
        "org.jetbrains.compose.ui",
        "org.jetbrains.compose.desktop",
        "org.jetbrains.compose.animation",
    )

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group in composeAlignedGroups) {
                useVersion(composeVersion)
                because("Align Compose Multiplatform libraries with the Gradle plugin version")
            }
        }
    }
}
