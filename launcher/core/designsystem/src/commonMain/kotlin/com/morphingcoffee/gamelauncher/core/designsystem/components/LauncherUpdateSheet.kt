package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing

data class LauncherUpdateSheetState(
    val visible: Boolean,
    val appVersion: String,
    val latestVersion: String?,
    val channelKey: String?,
    val fileSizeBytes: Long?,
    val errorMessage: String?,
    val isUpdateCharging: Boolean,
    val isUpdateDownloading: Boolean,
)

@Composable
fun LauncherUpdateSheet(
    state: LauncherUpdateSheetState,
    onDismiss: () -> Unit,
    onUpdateClicked: () -> Unit,
    onUpdateChargeComplete: () -> Unit,
    onReleaseNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return

    val latestVersion = state.latestVersion ?: return

    TerminalOverlay(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DisplayTitle(text = "LAUNCHER UPDATE")

        LauncherUpdateDetails(
            currentVersion = state.appVersion,
            latestVersion = latestVersion,
            channelKey = state.channelKey,
            fileSizeBytes = state.fileSizeBytes,
            errorMessage = state.errorMessage,
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
