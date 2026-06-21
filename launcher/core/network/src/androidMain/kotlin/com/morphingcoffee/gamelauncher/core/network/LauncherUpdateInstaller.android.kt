package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class LauncherUpdateInstallerImpl : LauncherUpdateInstaller {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    override suspend fun downloadAndApply(
        channelBuild: LauncherChannelBuild,
        versionLabel: String,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Launcher updates are desktop-only"))
}

actual fun createLauncherUpdateInstaller(downloadHttpClient: HttpClient): LauncherUpdateInstaller =
    LauncherUpdateInstallerImpl()
