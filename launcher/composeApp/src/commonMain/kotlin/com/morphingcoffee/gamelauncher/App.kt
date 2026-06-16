package com.morphingcoffee.gamelauncher

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.navigation.AppDestination
import com.morphingcoffee.gamelauncher.core.navigation.DebugNavigation
import com.morphingcoffee.gamelauncher.core.navigation.appNavigationConfig
import com.morphingcoffee.gamelauncher.feature.home.CatalogScreen
import com.morphingcoffee.gamelauncher.feature.home.CatalogScreenContent
import com.morphingcoffee.gamelauncher.feature.home.catalogPreviewState
import com.morphingcoffee.gamelauncher.feature.logs.LogsScreen
import com.morphingcoffee.gamelauncher.feature.settings.SettingsScreen
import com.morphingcoffee.gamelauncher.feature.settings.SettingsScreenContent
import com.morphingcoffee.gamelauncher.feature.settings.SettingsState
import com.morphingcoffee.gamelauncher.feature.settings.StorageScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun App() {
    AppNavigation()
}

private fun openLogsDestination(backStack: androidx.navigation3.runtime.NavBackStack<NavKey>) {
    if (backStack.lastOrNull() != AppDestination.Logs) {
        backStack.add(AppDestination.Logs)
    }
}

@Composable
internal fun AppNavigation(
    catalogContent: @Composable (onOpenSettings: () -> Unit) -> Unit = { onOpenSettings ->
        CatalogScreen(onOpenSettings = onOpenSettings)
    },
) {
    LauncherTheme {
        val backStack = rememberNavBackStack(appNavigationConfig, AppDestination.Home)
        val focusRequester = androidx.compose.runtime.remember { FocusRequester() }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        androidx.compose.runtime.LaunchedEffect(backStack) {
            DebugNavigation.openLogsRequests.collectLatest {
                openLogsDestination(backStack)
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (!LauncherMetadata.DEBUG_TOOLS_ENABLED) return@onPreviewKeyEvent false
                        if (event.type != KeyEventType.KeyDown || event.key != Key.F12) return@onPreviewKeyEvent false
                        DebugNavigation.requestOpenLogs()
                        true
                    },
        ) {
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
                                    onOpenStorage = {
                                        backStack.add(AppDestination.Storage)
                                    },
                                )
                            }
                        AppDestination.Storage ->
                            NavEntry(key) {
                                StorageScreen(
                                    onBack = {
                                        if (backStack.size > 1) backStack.removeLastOrNull()
                                    },
                                )
                            }
                        AppDestination.Logs ->
                            NavEntry(key) {
                                LogsScreen(
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
                onVersionPickerToggled = {},
                onVersionSelected = {},
                onDownloadClicked = {},
                onLaunchClicked = {},
                onOpenClicked = {},
                onLaunchChargeComplete = {},
                onUninstallClicked = {},
                onUninstallChargeComplete = {},
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
