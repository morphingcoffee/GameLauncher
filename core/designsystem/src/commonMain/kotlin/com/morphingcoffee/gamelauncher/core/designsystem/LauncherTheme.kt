package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val LauncherColorScheme =
    darkColorScheme(
        primary = Color(0xFF6C9EFF),
        onPrimary = Color(0xFF001B3D),
        secondary = Color(0xFFB8C8FF),
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        onBackground = Color(0xFFE6EDF3),
        onSurface = Color(0xFFE6EDF3),
    )

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            Text(
                text = "LauncherTheme preview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
