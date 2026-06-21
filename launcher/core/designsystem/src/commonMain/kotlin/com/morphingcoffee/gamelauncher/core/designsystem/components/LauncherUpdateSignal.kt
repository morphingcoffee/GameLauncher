package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.formatLauncherUpdateSignalLabel

@Composable
fun LauncherUpdateSignal(
    currentVersion: String,
    latestVersion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val label =
        formatLauncherUpdateSignalLabel(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isHovered = isHovered,
        )

    Row(
        modifier =
            modifier
                .hoverable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(LauncherSpacing.Sm),
    ) {
        MonoLabel(text = "·", muted = true)
        MonoLabel(text = label, accent = true)
    }
}
