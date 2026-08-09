package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme

/**
 * App-wide animated (or static) launcher background.
 *
 * Render as a **sibling behind** navigation content — do not wrap [NavDisplay] in a content
 * slot, or frame-driven background work can invalidate the UI tree (thumbnail stutter).
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

/**
 * Non-consuming pointer observer for the window host that sits above [LauncherBackground].
 * Updates [probe] without Compose state so navigation content is not recomposed on move.
 */
expect fun Modifier.observeLauncherBackgroundPointer(
    probe: LauncherBackgroundPointerProbe,
    enabled: Boolean,
): Modifier

/** Shared cursor probe written by [observeLauncherBackgroundPointer], read by the draw layer. */
class LauncherBackgroundPointerProbe {
    @Volatile
    var inside: Boolean = false

    @Volatile
    var x: Float = 0f

    @Volatile
    var y: Float = 0f
}

/**
 * Provided by the app host around the window content so the background draw layer can read
 * pointer position without owning hit-testing (avoids Exit when hovering UI).
 */
val LocalLauncherBackgroundPointerProbe =
    staticCompositionLocalOf<LauncherBackgroundPointerProbe?> { null }
