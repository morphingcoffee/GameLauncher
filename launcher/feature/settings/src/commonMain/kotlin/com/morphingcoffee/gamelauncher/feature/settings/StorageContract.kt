package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
import com.morphingcoffee.gamelauncher.core.designsystem.components.StorageChartAnimation
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

sealed interface StorageEvent {
    data object Started : StorageEvent

    data object Refresh : StorageEvent

    data object ClockTick : StorageEvent

    data class SegmentHovered(
        val segmentId: String?,
    ) : StorageEvent

    data class SegmentClicked(
        val segmentId: String,
    ) : StorageEvent

    data object CenterClicked : StorageEvent

    data object DialogDismissed : StorageEvent

    data object UninstallClicked : StorageEvent

    data object UninstallChargeComplete : StorageEvent

    data object UninstallAllClicked : StorageEvent

    data object UninstallAllChargeComplete : StorageEvent

    data object ChartAnimationFinished : StorageEvent

    data object ScreenHidden : StorageEvent
}

data class StorageSegmentUi(
    val gameId: String,
    val title: String,
    val version: String,
    val sizeBytes: Long,
    val shareFraction: Float,
    val pieSegment: PieSegment,
)

sealed interface StorageDialog {
    data class GameDetail(
        val segment: StorageSegmentUi,
    ) : StorageDialog

    data object UninstallAll : StorageDialog
}

data class StorageState(
    val isLoading: Boolean = true,
    val segments: List<StorageSegmentUi> = emptyList(),
    val totalBytes: Long = 0L,
    /** Breakdown list held steady during segment reflow so the pie layout does not jump. */
    val breakdownSegments: List<StorageSegmentUi>? = null,
    /** Center label totals held steady during segment reflow. */
    val centerDisplayTotalBytes: Long? = null,
    val centerDisplayInstallCount: Int? = null,
    val hoveredSegmentId: String? = null,
    val activeDialog: StorageDialog? = null,
    val isChargingUninstall: Boolean = false,
    val isUninstalling: Boolean = false,
    val chartAnimation: StorageChartAnimation? = null,
    val pendingUninstallAll: Boolean = false,
    val pendingUninstallGameId: String? = null,
    val errorMessage: String? = null,
    val appVersion: String = LauncherMetadata.VERSION,
    val platformLabel: String = "unknown",
    val clockText: String = "",
) {
    val statusLabel: String
        get() =
            when {
                isUninstalling -> "UNINSTALLING"
                isLoading -> "LOADING"
                errorMessage != null -> "ERROR"
                else -> "STORAGE"
            }

    val canUninstall: Boolean
        get() = !isLoading && !isUninstalling && !isChargingUninstall && chartAnimation == null

    val displayBreakdownSegments: List<StorageSegmentUi>
        get() = breakdownSegments ?: segments

    val displayCenterTotalBytes: Long
        get() = centerDisplayTotalBytes ?: totalBytes

    val displayCenterInstallCount: Int
        get() = centerDisplayInstallCount ?: segments.size
}

internal fun formatStoragePlatformLabel(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "windows-x64"
        PlatformKey.MACOS_ARM64 -> "macos-arm64"
        PlatformKey.MACOS_X64 -> "macos-x64"
        null -> "unknown"
        else -> platformKey
    }
