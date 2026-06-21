package com.morphingcoffee.gamelauncher.core.designsystem

import com.morphingcoffee.gamelauncher.core.model.LauncherVersion

data class LauncherHeaderVersionLabels(
    val marketingLabel: String,
    val buildLabel: String?,
)

fun formatLauncherHeaderVersion(version: String): LauncherHeaderVersionLabels {
    val parts = LauncherVersion.parse(version)
    val buildLabel = parts.buildNumber?.let { "build$it" }
    return LauncherHeaderVersionLabels(
        marketingLabel = "v${parts.marketing}",
        buildLabel = buildLabel,
    )
}

/** Full artifact label for update targets, e.g. `v0.0.1-build66`. */
fun formatLauncherVersionLabel(version: String): String {
    val parts = LauncherVersion.parse(version)
    return "v${LauncherVersion.formatWire(parts)}"
}

/** Sheet / detail comparison line using wire-format versions. */
fun formatLauncherVersionDelta(
    currentVersion: String,
    latestVersion: String,
): String {
    val current = LauncherVersion.formatWire(LauncherVersion.parse(currentVersion))
    val latest = LauncherVersion.formatWire(LauncherVersion.parse(latestVersion))
    return "$current → $latest"
}

fun formatLauncherUpdateSignalLabel(
    currentVersion: String,
    latestVersion: String,
    isHovered: Boolean,
): String {
    val targetLabel = formatLauncherVersionLabel(latestVersion)
    if (!isHovered) {
        return "LAUNCHER ↑ $targetLabel"
    }

    val currentParts = LauncherVersion.parse(currentVersion)
    val latestParts = LauncherVersion.parse(latestVersion)
    val hoverLabel =
        if (currentParts.buildNumber != null && latestParts.buildNumber != null) {
            "build${currentParts.buildNumber} → build${latestParts.buildNumber}"
        } else {
            targetLabel
        }
    return "[ LAUNCHER ↑ $hoverLabel ]"
}

/** About / info rows — wire format without a leading `v`. */
fun formatLauncherVersionInfoValue(version: String): String = LauncherVersion.formatWire(LauncherVersion.parse(version))
