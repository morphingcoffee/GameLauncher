package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus

sealed interface AboutEvent {
    data object Started : AboutEvent

    data object UpdateClicked : AboutEvent

    data object UpdateChargeComplete : AboutEvent

    data object GetLatestClicked : AboutEvent

    data object ClockTick : AboutEvent
}

data class AboutState(
    val appVersion: String = LauncherMetadata.VERSION,
    val platformLabel: String = "unknown",
    val clockText: String = "",
    val links: List<SettingsLink> = defaultSettingsLinks(),
    val updateEvaluation: LauncherUpdateEvaluation? = null,
    val isUpdateDownloading: Boolean = false,
    val isUpdateCharging: Boolean = false,
    val updateErrorMessage: String? = null,
    val downloadProgressFraction: Float? = null,
) {
    val showLatestRow: Boolean
        get() = updateEvaluation?.status == LauncherUpdateStatus.UpdateAvailable

    val channelLatestVersion: String?
        get() = updateEvaluation?.channelBuild?.version

    val showUpdateButton: Boolean
        get() = updateEvaluation?.status == LauncherUpdateStatus.UpdateAvailable
}

sealed interface AboutEffect {
    data class OpenUrl(
        val url: String,
    ) : AboutEffect
}
