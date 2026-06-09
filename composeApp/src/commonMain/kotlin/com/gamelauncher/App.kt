package com.gamelauncher

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gamelauncher.core.designsystem.LauncherTheme
import com.gamelauncher.core.navigation.AppDestination
import com.gamelauncher.core.navigation.appNavigationConfig
import com.gamelauncher.feature.home.HomeScreen

@Composable
fun App() {
    LauncherTheme {
        val backStack = rememberNavBackStack(appNavigationConfig, AppDestination.Home)

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    AppDestination.Home -> NavEntry(key) {
                        HomeScreen()
                    }
                    else -> error("Unknown destination: $key")
                }
            },
        )
    }
}
