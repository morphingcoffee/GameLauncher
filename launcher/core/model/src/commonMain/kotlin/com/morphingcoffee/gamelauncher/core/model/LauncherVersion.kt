package com.morphingcoffee.gamelauncher.core.model

data class LauncherVersionParts(
    val marketing: String,
    /** Set when the version string includes an explicit `-buildN` suffix. */
    val buildNumber: Int?,
)

object LauncherVersion {
    fun fullVersion(): String {
        val marketing = LauncherVersionGenerated.MARKETING_VERSION
        val buildNumber = LauncherVersionGenerated.BUILD_NUMBER
        return if (buildNumber.isNotEmpty()) {
            formatWire(marketing, buildNumber.toIntOrNull())
        } else {
            marketing
        }
    }

    fun parse(version: String): LauncherVersionParts {
        val trimmed = version.trim().removeSuffix("-dev")
        val buildIndex = trimmed.indexOf("-build")
        val marketing =
            if (buildIndex >= 0) {
                trimmed.substring(0, buildIndex)
            } else {
                trimmed
            }
        val buildNumber =
            if (buildIndex >= 0) {
                trimmed.substring(buildIndex + "-build".length).toIntOrNull()
                    ?: error("Invalid build suffix in version: $version")
            } else {
                null
            }
        return LauncherVersionParts(
            marketing = marketing,
            buildNumber = buildNumber,
        )
    }

    fun formatWire(parts: LauncherVersionParts): String = formatWire(parts.marketing, parts.buildNumber)

    fun formatWire(
        marketing: String,
        buildNumber: Int?,
    ): String =
        if (buildNumber != null) {
            "$marketing-build$buildNumber"
        } else {
            marketing
        }
}
