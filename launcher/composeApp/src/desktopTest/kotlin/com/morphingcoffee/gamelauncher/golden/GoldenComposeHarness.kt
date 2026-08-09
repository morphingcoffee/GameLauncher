package com.morphingcoffee.gamelauncher.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme

private const val GOLDEN_WIDTH = 1280
private const val GOLDEN_HEIGHT = 800

/** Fixed frame advances after setContent (16 ms frames) so layout settles without free-running animations. */
private const val GOLDEN_FRAME_ADVANCES = 3

@OptIn(ExperimentalTestApi::class)
internal fun assertGoldenScreenshot(
    name: String,
    content: @Composable () -> Unit,
) {
    assumeCanonicalGoldenHost()
    runDesktopComposeUiTest(width = GOLDEN_WIDTH, height = GOLDEN_HEIGHT) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 1f),
            ) {
                // Opaque plane — LauncherTheme Surface is transparent for the live app background.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(LauncherColors.Background),
                ) {
                    LauncherTheme(typography = goldenTypography()) {
                        content()
                    }
                }
            }
        }
        repeat(GOLDEN_FRAME_ADVANCES) {
            mainClock.advanceTimeByFrame()
        }
        val awt = onRoot().captureToImage().toAwtImage()
        assertMatchesGolden(name, awt)
    }
}
