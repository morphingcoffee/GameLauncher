package com.morphingcoffee.gamelauncher.feature.home

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import com.morphingcoffee.gamelauncher.core.network.InstallState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogContractTest {
    @Test
    fun showLauncherUpdateSignal_whenOptionalUpdateAvailable() {
        val state =
            CatalogState(
                updateEvaluation = optionalLauncherUpdate(),
            )

        assertTrue(state.showLauncherUpdateSignal)
        assertTrue(state.canTriggerLauncherUpdate)
        assertFalse(state.isUpdateGateActive)
    }

    @Test
    fun gameUpdateAvailable_whenInstalledVersionIsBehindLatest() {
        val state =
            CatalogState(
                games =
                    listOf(
                        GameCatalogEntry(
                            id = "alpha",
                            title = "Alpha",
                            description = "Test",
                            latestVersion = "2.0.0",
                            versionsUrl = "https://example.com/alpha/versions.json",
                            builds =
                                mapOf(
                                    "macos-arm64" to
                                        GameBuild(
                                            downloadUrl = "https://example.com/alpha.zip",
                                            executablePath = "Game.app/Contents/MacOS/Game",
                                            fileSizeBytes = 1024,
                                            sha256 = "abc",
                                        ),
                                ),
                        ),
                    ),
                selectedGameId = "alpha",
                installStatesByGameId =
                    mapOf(
                        "alpha" to
                            InstallState.Installed(
                                version = "1.0.0",
                                executablePath = "Game.app/Contents/MacOS/Game",
                            ),
                    ),
            )

        assertTrue(state.gameUpdateAvailable)
        assertTrue(state.gameHasUpdate("alpha"))
    }

    private fun optionalLauncherUpdate(): LauncherUpdateEvaluation =
        LauncherUpdateEvaluation(
            status = LauncherUpdateStatus.UpdateAvailable,
            channelKey = "macos-arm64-dmg",
            channelBuild =
                LauncherChannelBuild(
                    version = "0.0.1-build99",
                    artifactType = "dmg",
                    downloadUrl = "https://cdn.example/launcher.dmg",
                    fileSizeBytes = 100L,
                    sha256 = "abc",
                ),
        )
}
