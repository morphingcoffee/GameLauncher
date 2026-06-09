plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.navigation3.ui)
                implementation(libs.androidx.savedstate)
                implementation(libs.androidx.savedstate.serialization)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
