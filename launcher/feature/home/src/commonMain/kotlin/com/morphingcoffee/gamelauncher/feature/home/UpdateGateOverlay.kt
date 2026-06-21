package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateDetails
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus

@Composable
fun UpdateGateOverlay(
    state: CatalogState,
    onUpdateClicked: () -> Unit,
    onUpdateChargeComplete: () -> Unit,
    onGetLatestClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isUpdateGateActive) return

    val isManualOnly = state.updateEvaluation?.status == LauncherUpdateStatus.ManualUpdateRequired
    val showInAppUpdate =
        !isManualOnly &&
            state.updateEvaluation
                ?.channelBuild
                ?.downloadUrl
                ?.isNotBlank() == true
    val latestVersion = state.channelLatestVersion

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(LauncherColors.Background.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LauncherSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Lg),
        ) {
            DisplayTitle(text = "LAUNCHER UPDATE REQUIRED")

            MonoLabel(
                text = "This launcher build is below the minimum supported version.",
                muted = true,
            )

            if (isManualOnly) {
                MonoLabel(
                    text = "Install manually from GitHub Releases.",
                    muted = true,
                )
            }

            if (latestVersion != null) {
                LauncherUpdateDetails(
                    currentVersion = state.appVersion,
                    latestVersion = latestVersion,
                    channelKey = state.updateEvaluation?.channelKey,
                    fileSizeBytes = state.updateEvaluation?.channelBuild?.fileSizeBytes,
                    errorMessage = state.updateErrorMessage,
                )
            } else {
                state.updateErrorMessage?.let { message ->
                    MonoLabel(text = message, accent = true)
                }
            }

            when {
                showInAppUpdate -> {
                    TerminalButton(
                        label = "UPDATE LAUNCHER",
                        onClick = onUpdateClicked,
                        charging = state.isUpdateCharging,
                        onChargeComplete = onUpdateChargeComplete,
                        enabled = !state.isUpdateDownloading,
                    )
                }

                isManualOnly -> {
                    TerminalButton(
                        label = "[ GET LATEST ]",
                        onClick = onGetLatestClicked,
                    )
                }
            }

            if (showInAppUpdate && isManualOnly.not()) {
                TerminalButton(
                    label = "[ GET LATEST ]",
                    onClick = onGetLatestClicked,
                )
            }
        }
    }
}
