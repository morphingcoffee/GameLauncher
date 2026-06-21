package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.LauncherRuntime
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluator
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import com.morphingcoffee.gamelauncher.core.model.LauncherVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherUpdateRepository(
    private val manifestRepository: ManifestRepository,
    private val updateInstaller: LauncherUpdateInstaller,
) {
    private val _evaluation = MutableStateFlow<LauncherUpdateEvaluation?>(null)
    val evaluation: StateFlow<LauncherUpdateEvaluation?> = _evaluation.asStateFlow()

    val downloadProgress: StateFlow<DownloadProgress?> = updateInstaller.downloadProgress

    suspend fun refreshFromManifestLoad(result: ManifestLoadResult) {
        if (LauncherRuntime.isDevBuild()) {
            _evaluation.value = devSupportedEvaluation()
            return
        }

        _evaluation.value =
            when (result) {
                is ManifestLoadResult.Success -> {
                    val channelKey = LauncherInstallChannel.resolveChannelKey(result.manifest.launcher)
                    LauncherUpdateEvaluator.evaluate(
                        manifest = result.manifest,
                        runtimeVersion = LauncherVersion.fullVersion(),
                        channelKey = channelKey,
                    )
                }
                ManifestLoadResult.DecodeFailed -> LauncherUpdateEvaluator.manualUpdateRequired()
                ManifestLoadResult.SkippedInDevBuild -> devSupportedEvaluation()
            }
    }

    suspend fun loadAndRefresh(): ManifestLoadResult {
        if (LauncherRuntime.isDevBuild()) {
            _evaluation.value = devSupportedEvaluation()
            return ManifestLoadResult.SkippedInDevBuild
        }

        val result = manifestRepository.loadManifest()
        refreshFromManifestLoad(result)
        return result
    }

    suspend fun downloadAndApplyUpdate(): Result<Unit> {
        if (LauncherRuntime.isDevBuild()) {
            return Result.failure(IllegalStateException("Launcher updates are disabled in dev builds"))
        }

        val evaluation = _evaluation.value ?: return Result.failure(IllegalStateException("No update evaluation"))
        val channelBuild = evaluation.channelBuild ?: return Result.failure(IllegalStateException("No channel build"))
        return updateInstaller.downloadAndApply(
            channelBuild = channelBuild,
            versionLabel = channelBuild.version,
        )
    }

    fun releasesUrl(): String =
        _evaluation.value?.releaseNotesUrl?.takeIf { it.isNotBlank() }
            ?: LauncherMetadata.RELEASES_URL

    private fun devSupportedEvaluation(): LauncherUpdateEvaluation =
        LauncherUpdateEvaluation(status = LauncherUpdateStatus.Supported)
}
