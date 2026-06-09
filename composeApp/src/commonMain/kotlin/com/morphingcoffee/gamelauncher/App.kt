package com.morphingcoffee.gamelauncher

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.navigation.AppDestination
import com.morphingcoffee.gamelauncher.core.navigation.appNavigationConfig
import com.morphingcoffee.gamelauncher.feature.home.HomeScreen
import com.morphingcoffee.gamelauncher.feature.home.HomeScreenContent
import com.morphingcoffee.gamelauncher.feature.home.HomeState

@Composable
fun App() {
    AppNavigation()
}

@Composable
internal fun AppNavigation(
    homeContent: @Composable () -> Unit = { HomeScreen() },
) {
    LauncherTheme {
        val backStack = rememberNavBackStack(appNavigationConfig, AppDestination.Home)

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    AppDestination.Home -> NavEntry(key) {
                        homeContent()
                    }
                    else -> error("Unknown destination: $key")
                }
            },
        )
    }
}

@Preview(
    name = "App — home",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AppHomePreview() {
    AppNavigation(
        homeContent = { HomeScreenContent(state = HomeState()) },
    )
}
