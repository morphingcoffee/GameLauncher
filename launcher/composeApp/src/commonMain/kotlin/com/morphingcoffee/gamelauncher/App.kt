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

@Composable
fun App() {
    AppNavigation()
}

@Composable
internal fun AppNavigation(catalogContent: @Composable () -> Unit = { CatalogScreen() }) {
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
                            catalogContent()
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
        catalogContent = {
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
