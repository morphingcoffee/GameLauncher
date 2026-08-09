package com.morphingcoffee.gamelauncher.golden

import androidx.compose.runtime.Composable
import com.morphingcoffee.gamelauncher.feature.home.CatalogScreenContent
import com.morphingcoffee.gamelauncher.feature.home.CatalogState
import com.morphingcoffee.gamelauncher.feature.settings.AboutState
import com.morphingcoffee.gamelauncher.feature.settings.SettingsScreenContent
import com.morphingcoffee.gamelauncher.feature.settings.StorageScreenContent
import com.morphingcoffee.gamelauncher.feature.settings.StorageState
import kotlin.test.Test

/**
 * Visual regression goldens for stateless screen content.
 *
 * Canonical host: macOS arm64 (`macos-15`). Other hosts skip via [assumeCanonicalGoldenHost].
 * Regenerate: `./gradlew -p launcher :composeApp:desktopTest -PupdateGolden`
 */
class UiGoldenScreenshotsTest {
    @Test
    fun catalog_loading() = assertCatalogGolden("catalog_loading", catalogLoadingState())

    @Test
    fun catalog_error() = assertCatalogGolden("catalog_error", catalogErrorState())

    @Test
    fun catalog_empty() = assertCatalogGolden("catalog_empty", catalogEmptyState())

    @Test
    fun catalog_loaded() = assertCatalogGolden("catalog_loaded", catalogLoadedState())

    @Test
    fun catalog_update_gate() = assertCatalogGolden("catalog_update_gate", catalogUpdateGateState())

    @Test
    fun catalog_launcher_update_sheet() =
        assertCatalogGolden("catalog_launcher_update_sheet", catalogLauncherUpdateSheetState())

    @Test
    fun about_default() = assertAboutGolden("about_default", aboutDefaultState())

    @Test
    fun about_update_sheet() = assertAboutGolden("about_update_sheet", aboutUpdateSheetState())

    @Test
    fun storage_loading() = assertStorageGolden("storage_loading", storageLoadingState())

    @Test
    fun storage_loaded() = assertStorageGolden("storage_loaded", storageLoadedState())

    @Test
    fun storage_uninstall_all_dialog() =
        assertStorageGolden("storage_uninstall_all_dialog", storageUninstallAllDialogState())
}

private fun assertCatalogGolden(
    name: String,
    state: CatalogState,
) {
    assertGoldenScreenshot(name) {
        CatalogScreenContentFixture(state)
    }
}

private fun assertAboutGolden(
    name: String,
    state: AboutState,
) {
    assertGoldenScreenshot(name) {
        SettingsScreenContent(
            state = state,
            onBack = {},
        )
    }
}

private fun assertStorageGolden(
    name: String,
    state: StorageState,
) {
    assertGoldenScreenshot(name) {
        StorageScreenContent(
            state = state,
            onBack = {},
            onRefresh = {},
            onSegmentHover = {},
            onSegmentClick = {},
            onCenterClick = {},
            onDialogDismiss = {},
            onUninstallClicked = {},
            onUninstallChargeComplete = {},
            onUninstallAllClicked = {},
            onUninstallAllChargeComplete = {},
            onChartAnimationFinished = {},
        )
    }
}

@Composable
private fun CatalogScreenContentFixture(state: CatalogState) {
    CatalogScreenContent(
        state = state,
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
}
