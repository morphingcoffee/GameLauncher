package com.morphingcoffee.gamelauncher.core.model

object LauncherUpdateEvaluator {
    fun evaluate(
        manifest: Manifest,
        runtimeVersion: String,
        channelKey: String?,
    ): LauncherUpdateEvaluation {
        val releaseNotesUrl = manifest.launcher?.releaseNotesUrl

        if (VersionComparator.isLessThan(runtimeVersion, manifest.launcherMinimumVersion)) {
            val channelBuild = channelKey?.let { manifest.launcher?.channels?.get(it) }
            return LauncherUpdateEvaluation(
                status =
                    if (channelBuild != null && channelBuild.downloadUrl.isNotBlank()) {
                        LauncherUpdateStatus.UpdateRequired
                    } else {
                        LauncherUpdateStatus.ManualUpdateRequired
                    },
                channelKey = channelKey,
                channelBuild = channelBuild,
                releaseNotesUrl = releaseNotesUrl,
            )
        }

        val channelBuild =
            channelKey?.let { manifest.launcher?.channels?.get(it) }
                ?: return LauncherUpdateEvaluation(
                    status = LauncherUpdateStatus.Supported,
                    channelKey = channelKey,
                    releaseNotesUrl = releaseNotesUrl,
                )

        return if (VersionComparator.isLessThan(runtimeVersion, channelBuild.version)) {
            LauncherUpdateEvaluation(
                status = LauncherUpdateStatus.UpdateAvailable,
                channelKey = channelKey,
                channelBuild = channelBuild,
                releaseNotesUrl = releaseNotesUrl,
            )
        } else {
            LauncherUpdateEvaluation(
                status = LauncherUpdateStatus.Supported,
                channelKey = channelKey,
                channelBuild = channelBuild,
                releaseNotesUrl = releaseNotesUrl,
            )
        }
    }

    fun manualUpdateRequired(): LauncherUpdateEvaluation =
        LauncherUpdateEvaluation(
            status = LauncherUpdateStatus.ManualUpdateRequired,
            releaseNotesUrl = "${LauncherMetadata.REPOSITORY_URL}/releases",
        )
}
