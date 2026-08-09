package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        StaticTerminalBackground(modifier = Modifier.fillMaxSize())
        content()
    }
}
