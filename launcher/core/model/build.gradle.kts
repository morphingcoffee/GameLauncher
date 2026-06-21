import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin/commonMain"))
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

val generateLauncherVersion by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/kotlin/commonMain/com/morphingcoffee/gamelauncher/core/model")
    val marketingVersion =
        (findProperty("launcherMarketingVersion") as String?)
            ?.trim()
            .takeUnless { it.isNullOrEmpty() }
            ?: error("launcherMarketingVersion is not set in gradle.properties")
    val buildNumber =
        (findProperty("buildNumber") as String?)
            ?.trim()
            .orEmpty()
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("LauncherVersionGenerated.kt").writeText(
            """
            package com.morphingcoffee.gamelauncher.core.model

            internal object LauncherVersionGenerated {
                const val MARKETING_VERSION: String = "$marketingVersion"
                const val BUILD_NUMBER: String = "$buildNumber"
            }
            """.trimIndent() + "\n",
        )
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateLauncherVersion)
}

android {
    namespace = "com.morphingcoffee.gamelauncher.core.model"
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
