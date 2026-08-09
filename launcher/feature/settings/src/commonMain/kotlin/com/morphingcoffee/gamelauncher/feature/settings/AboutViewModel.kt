package com.morphingcoffee.gamelauncher.feature.settings

import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.LauncherUpdateRepository
import com.morphingcoffee.gamelauncher.core.telemetry.CrashReporting
import com.morphingcoffee.gamelauncher.core.telemetry.TelemetryPreferences
import com.morphingcoffee.gamelauncher.core.telemetry.TelemetryPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel(
    private val launcherUpdateRepository: LauncherUpdateRepository,
    private val telemetryPreferencesStore: TelemetryPreferencesStore,
) : MviViewModel<AboutState, AboutEvent, AboutEffect>(
        initialState =
            AboutState(
                platformLabel = formatPlatformLabel(PlatformKey.current()),
                releasesUrl = launcherUpdateRepository.releasesUrl(),
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
                val prefs = telemetryPreferencesStore.load()
                CrashReporting.updatePreferences(prefs)
                updateState {
                    copy(
                        clockText = platformClockText(),
                        appVersion = LauncherMetadata.VERSION,
                        releasesUrl = launcherUpdateRepository.releasesUrl(),
                        updateEvaluation = launcherUpdateRepository.evaluation.value,
                        sendCrashReports = prefs.sendCrashReports,
                        shareExtendedDiagnostics = prefs.shareExtendedDiagnostics,
                    )
                }
            }

            AboutEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            AboutEvent.LauncherUpdateSignalClicked -> {
                if (!state.value.showLauncherUpdateSignal) return
                updateState { copy(isLauncherUpdateSheetVisible = true) }
            }

            AboutEvent.LauncherUpdateSheetDismissed -> {
                updateState { copy(isLauncherUpdateSheetVisible = false) }
            }

            AboutEvent.UpdateClicked -> {
                if (!state.value.showLauncherUpdateSignal) return
                if (state.value.isUpdateDownloading) return
                updateState { copy(isUpdateCharging = true, updateErrorMessage = null) }
            }

            AboutEvent.UpdateChargeComplete -> downloadAndApplyUpdate()

            AboutEvent.ReleaseNotesClicked -> {
                sendEffect(AboutEffect.OpenUrl(launcherUpdateRepository.releasesUrl()))
            }

            AboutEvent.SendCrashReportsToggled -> {
                val enabled = !state.value.sendCrashReports
                val next =
                    TelemetryPreferences(
                        sendCrashReports = enabled,
                        shareExtendedDiagnostics =
                            if (enabled) {
                                state.value.shareExtendedDiagnostics
                            } else {
                                false
                            },
                    )
                persistPreferences(next)
            }

            AboutEvent.ShareExtendedDiagnosticsToggled -> {
                if (!state.value.sendCrashReports) return
                val next =
                    TelemetryPreferences(
                        sendCrashReports = true,
                        shareExtendedDiagnostics = !state.value.shareExtendedDiagnostics,
                    )
                persistPreferences(next)
            }
        }
    }

    private fun persistPreferences(preferences: TelemetryPreferences) {
        updateState {
            copy(
                sendCrashReports = preferences.sendCrashReports,
                shareExtendedDiagnostics = preferences.shareExtendedDiagnostics,
            )
        }
        CrashReporting.updatePreferences(preferences)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                telemetryPreferencesStore.save(preferences)
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
                updateState {
                    copy(
                        isUpdateDownloading = false,
                        isLauncherUpdateSheetVisible = false,
                    )
                }
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
