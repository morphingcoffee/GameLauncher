package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherUpdateEvaluatorTest {
    private val channelBuild =
        LauncherChannelBuild(
            version = "0.0.1-build51",
            artifactType = "msi",
            downloadUrl = "https://cdn.example/launcher.msi",
            fileSizeBytes = 100L,
            sha256 = "abc",
        )

    private fun manifest(
        minimum: String = "0.0.1",
        channels: Map<String, LauncherChannelBuild> = mapOf(LauncherChannelKey.WINDOWS_X64_MSI to channelBuild),
    ) = Manifest(
        launcherMinimumVersion = minimum,
        launcher =
            LauncherRelease(
                releaseNotesUrl = "https://github.com/morphingcoffee/GameLauncher/releases",
                channels = channels,
            ),
        games = emptyList(),
    )

    @Test
    fun belowMinimum_requiresUpdateWhenChannelPresent() {
        val evaluation =
            LauncherUpdateEvaluator.evaluate(
                manifest = manifest(minimum = "0.0.1-build50"),
                runtimeVersion = "0.0.1-build48",
                channelKey = LauncherChannelKey.WINDOWS_X64_MSI,
            )

        assertEquals(LauncherUpdateStatus.UpdateRequired, evaluation.status)
    }

    @Test
    fun belowMinimum_manualWhenChannelMissing() {
        val evaluation =
            LauncherUpdateEvaluator.evaluate(
                manifest = manifest(minimum = "0.0.1-build50", channels = emptyMap()),
                runtimeVersion = "0.0.1-build48",
                channelKey = LauncherChannelKey.WINDOWS_X64_MSI,
            )

        assertEquals(LauncherUpdateStatus.ManualUpdateRequired, evaluation.status)
    }

    @Test
    fun aboveMinimum_belowChannelVersion_isUpdateAvailable() {
        val evaluation =
            LauncherUpdateEvaluator.evaluate(
                manifest = manifest(),
                runtimeVersion = "0.0.1-build48",
                channelKey = LauncherChannelKey.WINDOWS_X64_MSI,
            )

        assertEquals(LauncherUpdateStatus.UpdateAvailable, evaluation.status)
    }

    @Test
    fun upToDate_isSupported() {
        val evaluation =
            LauncherUpdateEvaluator.evaluate(
                manifest = manifest(),
                runtimeVersion = "0.0.1-build51",
                channelKey = LauncherChannelKey.WINDOWS_X64_MSI,
            )

        assertEquals(LauncherUpdateStatus.Supported, evaluation.status)
    }
}
