package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule
import com.morphingcoffee.gamelauncher.core.designsystem.formatLauncherHeaderVersion

@Composable
fun AppHeader(
    appVersion: String,
    platformLabel: String,
    modifier: Modifier = Modifier,
    launcherUpdateSlot: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TerminalRule()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(LauncherSpacing.HeaderHeight)
                    .padding(horizontal = LauncherSpacing.Lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm)) {
                MonoLabel(text = "MC.GAME.LAUNCHER")
                MonoLabel(text = "·", muted = true)
                val versionLabels = formatLauncherHeaderVersion(appVersion)
                MonoLabel(text = versionLabels.marketingLabel, muted = true)
                versionLabels.buildLabel?.let { buildLabel ->
                    MonoLabel(text = "·", muted = true)
                    MonoLabel(text = buildLabel, muted = true)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                launcherUpdateSlot?.invoke()
                MonoLabel(text = "sys:$platformLabel", muted = true)
            }
        }
        TerminalRule()
    }
}
