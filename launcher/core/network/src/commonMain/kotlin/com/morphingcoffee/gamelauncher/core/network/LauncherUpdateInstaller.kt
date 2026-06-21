package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import kotlinx.coroutines.flow.StateFlow

interface LauncherUpdateInstaller {
    val downloadProgress: StateFlow<DownloadProgress?>

    suspend fun downloadAndApply(
        channelBuild: LauncherChannelBuild,
        versionLabel: String,
    ): Result<Unit>
}

expect fun createLauncherUpdateInstaller(downloadHttpClient: io.ktor.client.HttpClient): LauncherUpdateInstaller
