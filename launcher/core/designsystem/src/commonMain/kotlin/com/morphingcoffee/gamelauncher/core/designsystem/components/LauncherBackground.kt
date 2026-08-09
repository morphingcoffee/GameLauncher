package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme

/**
 * App-wide animated (or static) launcher background.
 *
 * Optional [timeSeconds] / [pointerNormalized] keep golden-image snapshots deterministic later
 * (issue #93). When null, desktop uses a live clock and non-consuming pointer tracking.
 *
 * [pointerNormalized] is in Shadertoy-style centered coords (x scaled by aspect, y by height).
 */
@Composable
expect fun LauncherBackground(
    theme: LauncherBackgroundTheme,
    modifier: Modifier = Modifier,
    timeSeconds: Float? = null,
    pointerNormalized: Offset? = null,
)
