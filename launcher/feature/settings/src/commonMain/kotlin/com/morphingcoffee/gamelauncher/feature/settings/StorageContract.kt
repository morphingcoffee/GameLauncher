package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
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
    val hoveredSegmentId: String? = null,
    val activeDialog: StorageDialog? = null,
    val isChargingUninstall: Boolean = false,
    val isUninstalling: Boolean = false,
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
                else -> "STORAGE"
            }

    val canUninstall: Boolean
        get() = !isLoading && !isUninstalling && !isChargingUninstall
}

internal fun formatStoragePlatformLabel(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "windows-x64"
        PlatformKey.MACOS_ARM64 -> "macos-arm64"
        PlatformKey.MACOS_X64 -> "macos-x64"
        null -> "unknown"
        else -> platformKey
    }
