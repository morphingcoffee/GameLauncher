package com.morphingcoffee.gamelauncher.golden

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTypography

/**
 * Bundled OFL fonts under `desktopTest/resources/fonts/` so goldens do not pick up
 * host `FontFamily.Monospace` / `SansSerif` faces.
 *
 * Weights match production usage: mono 400 + 500, sans 300 (displayLarge Light).
 */
internal fun goldenTypography(): Typography {
    val mono =
        FontFamily(
            Font(
                identity = "JetBrainsMono-Regular",
                data = fontBytes("fonts/JetBrainsMono-Regular.ttf"),
                weight = FontWeight.Normal,
            ),
            Font(
                identity = "JetBrainsMono-Medium",
                data = fontBytes("fonts/JetBrainsMono-Medium.ttf"),
                weight = FontWeight.Medium,
            ),
        )
    val display =
        FontFamily(
            Font(
                identity = "Inter-Light",
                data = fontBytes("fonts/Inter-Light.ttf"),
                weight = FontWeight.Light,
            ),
        )

    // Material3 Typography(fontFamily=…) applies mono to every style that does not set its own
    // family (including emphasized tokens). Overlay the four LauncherTypography styles with
    // bundled faces so production metrics stay aligned while fonts stay deterministic.
    return Typography(
        fontFamily = mono,
        displayLarge = LauncherTypography.displayLarge.copy(fontFamily = display),
        titleMedium = LauncherTypography.titleMedium.copy(fontFamily = mono),
        bodyMedium = LauncherTypography.bodyMedium.copy(fontFamily = mono),
        labelSmall = LauncherTypography.labelSmall.copy(fontFamily = mono),
    )
}

private fun fontBytes(classpathPath: String): ByteArray {
    val stream =
        Thread.currentThread().contextClassLoader.getResourceAsStream(classpathPath)
            ?: error("Missing golden font on classpath: $classpathPath")
    return stream.use { it.readBytes() }
}
