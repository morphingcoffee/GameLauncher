package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateDetails
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalLinkRow
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalOverlay

@Composable
fun LauncherUpdateSheet(
    state: CatalogState,
    onDismiss: () -> Unit,
    onUpdateClicked: () -> Unit,
    onUpdateChargeComplete: () -> Unit,
    onReleaseNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLauncherUpdateSheetVisible) return

    val latestVersion = state.channelLatestVersion ?: return

    TerminalOverlay(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DisplayTitle(text = "LAUNCHER UPDATE")

        LauncherUpdateDetails(
            currentVersion = state.appVersion,
            latestVersion = latestVersion,
            channelKey = state.updateEvaluation?.channelKey,
            fileSizeBytes = state.updateEvaluation?.channelBuild?.fileSizeBytes,
            errorMessage = state.updateErrorMessage,
        )

        TerminalButton(
            label = "UPDATE LAUNCHER",
            onClick = onUpdateClicked,
            charging = state.isUpdateCharging,
            onChargeComplete = onUpdateChargeComplete,
            enabled = !state.isUpdateDownloading,
            modifier = Modifier.fillMaxWidth(),
        )

        TerminalLinkRow(
            label = "NOTES",
            linkText = "RELEASE NOTES →",
            onClick = onReleaseNotesClicked,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
        ) {
            TerminalButton(
                label = "[ DISMISS ]",
                onClick = onDismiss,
                enabled = !state.isUpdateDownloading && !state.isUpdateCharging,
            )
        }
    }
}
