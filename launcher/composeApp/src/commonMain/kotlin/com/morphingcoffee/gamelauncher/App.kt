package com.morphingcoffee.gamelauncher

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.navigation.AppDestination
import com.morphingcoffee.gamelauncher.core.navigation.appNavigationConfig
import com.morphingcoffee.gamelauncher.feature.home.CatalogScreen
import com.morphingcoffee.gamelauncher.feature.home.CatalogScreenContent
import com.morphingcoffee.gamelauncher.feature.home.catalogPreviewState
import com.morphingcoffee.gamelauncher.feature.settings.SettingsScreen
import com.morphingcoffee.gamelauncher.feature.settings.SettingsScreenContent
import com.morphingcoffee.gamelauncher.feature.settings.SettingsState

@Composable
fun App() {
    AppNavigation()
}

@Composable
internal fun AppNavigation(
    catalogContent: @Composable (onOpenSettings: () -> Unit) -> Unit = { onOpenSettings ->
        CatalogScreen(onOpenSettings = onOpenSettings)
    },
) {
    LauncherTheme {
        val backStack = rememberNavBackStack(appNavigationConfig, AppDestination.Home)

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) backStack.removeLastOrNull()
            },
            entryProvider = { key ->
                when (key) {
                    AppDestination.Home ->
                        NavEntry(key) {
                            catalogContent {
                                backStack.add(AppDestination.Settings)
                            }
                        }
                    AppDestination.Settings ->
                        NavEntry(key) {
                            SettingsScreen(
                                onBack = {
                                    if (backStack.size > 1) backStack.removeLastOrNull()
                                },
                            )
                        }
                    else -> error("Unknown destination: $key")
                }
            },
        )
    }
}

@Preview(
    name = "App — catalog",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AppCatalogPreview() {
    AppNavigation(
        catalogContent = { _ ->
            CatalogScreenContent(
                state = catalogPreviewState(),
                requestRosterFocus = false,
                onRosterFocusHandled = {},
                onGameSelected = {},
                onMoveSelection = {},
                onLaunchClicked = {},
                onLaunchChargeComplete = {},
                onAmbientColorExtracted = { _, _ -> },
                onRetryLoad = {},
            )
        },
    )
}

@Preview(
    name = "App — settings",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AppSettingsPreview() {
    LauncherTheme {
        SettingsScreenContent(
            state =
                SettingsState(
                    platformLabel = "macos-arm64",
                    clockText = "12:34:56",
                ),
            onBack = {},
        )
    }
}
