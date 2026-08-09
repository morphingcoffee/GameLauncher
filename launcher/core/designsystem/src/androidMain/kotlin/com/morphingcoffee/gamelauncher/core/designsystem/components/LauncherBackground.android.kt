package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme

@Composable
actual fun LauncherBackground(
    theme: LauncherBackgroundTheme,
    modifier: Modifier,
    timeSeconds: Float?,
    pointerNormalized: Offset?,
) {
    StaticTerminalBackground(modifier = modifier)
}

actual fun Modifier.observeLauncherBackgroundPointer(
    probe: LauncherBackgroundPointerProbe,
    enabled: Boolean,
): Modifier = this
