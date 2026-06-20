package com.morphingcoffee.gamelauncher.core.model

object LauncherVersion {
    fun fullVersion(): String {
        val marketing = LauncherVersionGenerated.MARKETING_VERSION
        val buildNumber = LauncherVersionGenerated.BUILD_NUMBER
        return if (buildNumber.isNotEmpty()) {
            "$marketing-build$buildNumber"
        } else {
            marketing
        }
    }
}
