package com.morphingcoffee.gamelauncher.feature.settings

import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.LauncherUpdateRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AboutViewModel(
    private val launcherUpdateRepository: LauncherUpdateRepository,
) : MviViewModel<AboutState, AboutEvent, AboutEffect>(
        initialState =
            AboutState(
                platformLabel = formatPlatformLabel(PlatformKey.current()),
            ),
    ) {
    init {
        launcherUpdateRepository.evaluation
            .onEach { evaluation ->
                updateState {
                    copy(
                        updateEvaluation = evaluation,
                        appVersion = LauncherMetadata.VERSION,
                    )
                }
            }.launchIn(viewModelScope)

        launcherUpdateRepository.downloadProgress
            .onEach { progress ->
                updateState {
                    if (progress == null) {
                        copy(
                            downloadProgressFraction = null,
                            isUpdateDownloading = false,
                        )
                    } else {
                        copy(
                            downloadProgressFraction = progress.fraction,
                            isUpdateDownloading = true,
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: AboutEvent) {
        when (event) {
            AboutEvent.Started -> {
                updateState {
                    copy(
                        clockText = platformClockText(),
                        appVersion = LauncherMetadata.VERSION,
                        updateEvaluation = launcherUpdateRepository.evaluation.value,
                    )
                }
            }

            AboutEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            AboutEvent.UpdateClicked -> {
                if (!state.value.showUpdateButton) return
                if (state.value.isUpdateDownloading) return
                updateState { copy(isUpdateCharging = true, updateErrorMessage = null) }
            }

            AboutEvent.UpdateChargeComplete -> downloadAndApplyUpdate()

            AboutEvent.GetLatestClicked -> {
                sendEffect(AboutEffect.OpenUrl(launcherUpdateRepository.releasesUrl()))
            }
        }
    }

    private fun downloadAndApplyUpdate() {
        viewModelScope.launch {
            updateState {
                copy(
                    isUpdateCharging = false,
                    isUpdateDownloading = true,
                    updateErrorMessage = null,
                )
            }

            try {
                AppLog.i("About", "Starting launcher update download")
                launcherUpdateRepository
                    .downloadAndApplyUpdate()
                    .onSuccess {
                        AppLog.i("About", "Launcher update handoff complete")
                    }.onFailure { error ->
                        AppLog.e("About", "Launcher update failed", error)
                        updateState {
                            copy(
                                updateErrorMessage = error.message ?: "Update failed",
                            )
                        }
                    }
            } finally {
                updateState { copy(isUpdateDownloading = false) }
            }
        }
    }
}

private fun formatPlatformLabel(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "windows-x64"
        PlatformKey.MACOS_ARM64 -> "macos-arm64"
        PlatformKey.MACOS_X64 -> "macos-x64"
        null -> "unknown"
        else -> platformKey
    }
