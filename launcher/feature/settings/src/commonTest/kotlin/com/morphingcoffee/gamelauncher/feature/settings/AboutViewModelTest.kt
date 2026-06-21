package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AboutViewModelTest {
    @Test
    fun aboutState_showsUpdateWhenEvaluationIsUpdateAvailable() {
        val state =
            AboutState(
                updateEvaluation =
                    LauncherUpdateEvaluation(
                        status = LauncherUpdateStatus.UpdateAvailable,
                        channelKey = "windows-x64-msi",
                        channelBuild =
                            LauncherChannelBuild(
                                version = "9.9.9-build99",
                                artifactType = "msi",
                                downloadUrl = "https://example.com/update.msi",
                                fileSizeBytes = 100,
                                sha256 = "abc",
                            ),
                    ),
            )

        assertTrue(state.showUpdateButton)
        assertTrue(state.showLatestRow)
        assertEquals("9.9.9-build99", state.channelLatestVersion)
    }

    @Test
    fun aboutState_hidesUpdateWhenSupported() {
        val state =
            AboutState(
                updateEvaluation =
                    LauncherUpdateEvaluation(
                        status = LauncherUpdateStatus.Supported,
                    ),
            )

        assertFalse(state.showUpdateButton)
        assertFalse(state.showLatestRow)
    }
}
