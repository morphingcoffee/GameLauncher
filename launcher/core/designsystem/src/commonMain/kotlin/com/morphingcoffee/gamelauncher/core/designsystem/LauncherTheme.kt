package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel

private val LauncherColorScheme =
    darkColorScheme(
        primary = LauncherColors.Primary,
        onPrimary = LauncherColors.OnAccent,
        secondary = LauncherColors.Secondary,
        background = LauncherColors.Background,
        surface = LauncherColors.Surface,
        onBackground = LauncherColors.OnBackground,
        onSurface = LauncherColors.OnSurface,
    )

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = LauncherTypography,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = LauncherColors.Background) {
            content()
        }
    }
}

@Preview(
    name = "Launcher theme",
    widthDp = 480,
    heightDp = 320,
    showBackground = true,
)
@Composable
private fun LauncherThemePreview() {
    LauncherTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            MonoLabel(
                text = "LauncherTheme preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
