package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.core.network.LAUNCHER_UPDATE_PROGRESS_ID
import com.morphingcoffee.gamelauncher.core.network.LauncherUpdateRepository
import com.morphingcoffee.gamelauncher.core.network.ManifestLoadResult
import com.morphingcoffee.gamelauncher.core.network.SimulatedLaunchException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val gameCatalogRepository: GameCatalogDataSource,
    private val launcherUpdateRepository: LauncherUpdateRepository,
) : MviViewModel<CatalogState, CatalogEvent, CatalogEffect>(
        initialState =
            CatalogState(
                platformKey = PlatformKey.current(),
            ),
    ) {
    private var installProbeJob: Job? = null
    private var catalogLoadJob: Job? = null
    private var catalogLoadEpoch = 0

    /** In-flight game download id — survives selection changes (unlike [CatalogState.isDownloading]). */
    private var activeDownloadGameId: String? = null

    /** In-flight game uninstall id — survives selection changes (unlike [CatalogState.isUninstalling]). */
    private var activeUninstallGameId: String? = null

    init {
        gameCatalogRepository.downloadProgress
            .onEach { progress ->
                if (progress?.gameId == LAUNCHER_UPDATE_PROGRESS_ID) return@onEach
                updateState {
                    if (progress == null) {
                        copy(
                            statusLabel =
                                if (isChargingLaunch || isLaunching || isUpdateDownloading) {
                                    statusLabel
                                } else {
                                    "READY"
                                },
                            downloadProgressFraction =
                                if (isUpdateDownloading) {
                                    downloadProgressFraction
                                } else {
                                    null
                                },
                        )
                    } else {
                        val statusLabel =
                            if (progress.fraction >= 1f) {
                                "GAME · EXTRACTING"
                            } else {
                                "GAME · DOWNLOADING"
                            }
                        copy(
                            statusLabel = statusLabel,
                            downloadProgressFraction = progress.fraction,
                        )
                    }
                }
            }.launchIn(viewModelScope)

        launcherUpdateRepository.evaluation
            .onEach { evaluation ->
                updateState { copy(updateEvaluation = evaluation) }
            }.launchIn(viewModelScope)

        launcherUpdateRepository.downloadProgress
            .onEach { progress ->
                updateState {
                    if (progress == null) {
                        copy(
                            downloadProgressFraction = if (isDownloading) downloadProgressFraction else null,
                            isUpdateDownloading = false,
                            statusLabel =
                                if (isUpdateGateActive) {
                                    "LAUNCHER · UPDATE REQUIRED"
                                } else if (isDownloading) {
                                    statusLabel
                                } else {
                                    "READY"
                                },
                        )
                    } else {
                        copy(
                            downloadProgressFraction = progress.fraction,
                            isUpdateDownloading = true,
                            statusLabel =
                                if (progress.fraction >= 1f) {
                                    "LAUNCHER · APPLYING"
                                } else {
                                    "LAUNCHER · UPDATING"
                                },
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Started -> {
                updateState { copy(clockText = platformClockText()) }
                // Home is disposed while a secondary destination is on top; on return CatalogScreen
                // fires Started again. Skip reload when games are already held or a load is in
                // flight — a fresh load flashes isLoading / clears install probes and remounts
                // ThumbnailImage.
                ensureCatalogLoaded()
                sendEffect(CatalogEffect.RequestFocusRoster)
            }

            CatalogEvent.OpenClicked -> openWebGame()

            CatalogEvent.RetryLoad -> loadCatalog(showLoading = true)

            CatalogEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            is CatalogEvent.GameSelected -> {
                if (event.gameId == state.value.selectedGameId) return
                selectGame(event.gameId)
            }

            is CatalogEvent.MoveSelection -> moveSelection(event.delta)

            is CatalogEvent.AmbientColorExtracted -> {
                val selected = state.value.selectedGame ?: return
                if (event.imageUrl != selected.thumbnailUrl) return
                updateState { copy(ambientColor = event.color) }
            }

            CatalogEvent.VersionPickerToggled -> toggleVersionPicker()

            is CatalogEvent.VersionSelected -> selectVersion(event.version)

            CatalogEvent.DownloadClicked -> downloadSelectedVersion()

            CatalogEvent.LaunchClicked -> {
                if (state.value.displayBuild == null) return
                if (state.value.isInstallStatePending) return
                if (!state.value.isInstalledForDisplay) return
                updateState { copy(isChargingLaunch = true, launchErrorMessage = null) }
            }

            CatalogEvent.LaunchChargeComplete -> launchSelectedGame()

            CatalogEvent.UninstallClicked -> {
                if (!state.value.canUninstall) return
                AppLog.i("Catalog", "Uninstall charge started for ${state.value.selectedGameId}")
                updateState { copy(isChargingUninstall = true, launchErrorMessage = null) }
            }

            CatalogEvent.UninstallChargeComplete -> {
                AppLog.i("Catalog", "Uninstall charge complete for ${state.value.selectedGameId}")
                uninstallSelectedGame()
            }

            CatalogEvent.UpdateClicked -> {
                if (!state.value.canTriggerLauncherUpdate) return
                if (state.value.isUpdateDownloading) return
                updateState { copy(isUpdateCharging = true, updateErrorMessage = null) }
            }

            CatalogEvent.UpdateChargeComplete -> downloadAndApplyUpdate()

            CatalogEvent.LauncherUpdateSignalClicked -> {
                if (!state.value.showOptionalUpdateHint) return
                updateState { copy(isLauncherUpdateSheetVisible = true) }
            }

            CatalogEvent.LauncherUpdateSheetDismissed -> {
                updateState { copy(isLauncherUpdateSheetVisible = false) }
            }

            CatalogEvent.GetLatestClicked -> {
                sendEffect(CatalogEffect.OpenUrl(launcherUpdateRepository.releasesUrl()))
            }
        }
    }

    private fun ensureCatalogLoaded() {
        if (catalogLoadJob?.isActive == true) return
        if (state.value.games.isNotEmpty()) return
        loadCatalog(showLoading = true)
    }

    private fun loadCatalog(showLoading: Boolean = true) {
        val epoch = ++catalogLoadEpoch
        catalogLoadJob?.cancel()
        catalogLoadJob =
            viewModelScope.launch {
                updateState {
                    copy(
                        isLoading = if (showLoading) true else isLoading,
                        errorMessage = null,
                        statusLabel = if (showLoading) "LOADING" else statusLabel,
                        appVersion = LauncherMetadata.VERSION,
                    )
                }

                try {
                    val result = launcherUpdateRepository.loadAndRefresh()
                    ensureActive()
                    if (epoch != catalogLoadEpoch) return@launch

                    val evaluation = launcherUpdateRepository.evaluation.value
                    updateState { copy(updateEvaluation = evaluation) }

                    if (state.value.isUpdateGateActive) {
                        updateState {
                            copy(
                                isLoading = false,
                                statusLabel = "LAUNCHER · UPDATE REQUIRED",
                            )
                        }
                        return@launch
                    }

                    when (result) {
                        is ManifestLoadResult.Success -> {
                            if (epoch != catalogLoadEpoch) return@launch
                            applyLoadedGames(result.manifest.games, loadEpoch = epoch)
                        }

                        ManifestLoadResult.SkippedInDevBuild -> {
                            gameCatalogRepository
                                .loadCatalog()
                                .onSuccess { games ->
                                    if (epoch != catalogLoadEpoch) return@launch
                                    applyLoadedGames(games, loadEpoch = epoch)
                                }.onFailure { catalogError ->
                                    if (epoch != catalogLoadEpoch) return@launch
                                    updateState {
                                        copy(
                                            isLoading = false,
                                            errorMessage = catalogError.message ?: "Failed to load catalog",
                                            statusLabel = "ERROR",
                                        )
                                    }
                                }
                        }

                        ManifestLoadResult.DecodeFailed -> {
                            if (epoch != catalogLoadEpoch) return@launch
                            updateState {
                                copy(
                                    isLoading = false,
                                    statusLabel = "LAUNCHER · UPDATE REQUIRED",
                                )
                            }
                        }
                    }
                } catch (error: Throwable) {
                    ensureActive()
                    if (epoch != catalogLoadEpoch) return@launch
                    gameCatalogRepository
                        .loadCatalog()
                        .onSuccess { games ->
                            if (epoch != catalogLoadEpoch) return@launch
                            applyLoadedGames(games, loadEpoch = epoch)
                        }.onFailure { catalogError ->
                            if (epoch != catalogLoadEpoch) return@launch
                            updateState {
                                copy(
                                    isLoading = false,
                                    errorMessage =
                                        catalogError.message ?: error.message ?: "Failed to load catalog",
                                    statusLabel = "ERROR",
                                )
                            }
                        }
                }
            }
    }

    private fun applyLoadedGames(
        games: List<GameCatalogEntry>,
        loadEpoch: Int = catalogLoadEpoch,
    ) {
        if (loadEpoch != catalogLoadEpoch) return

        val selectedGameId = state.value.selectedGameId ?: games.firstOrNull()?.id
        installProbeJob?.cancel()

        updateState {
            copy(
                isLoading = false,
                games = games,
                selectedGameId = selectedGameId,
                statusLabel = "READY",
                errorMessage = null,
                installStatesByGameId = emptyMap(),
                installState = InstallState.Unknown,
                onDiskSizeBytes = null,
            )
        }

        val probeOrder =
            buildList {
                selectedGameId?.let { add(it) }
                games.forEach { game ->
                    if (game.id != selectedGameId) {
                        add(game.id)
                    }
                }
            }
        if (probeOrder.isEmpty()) return

        installProbeJob =
            viewModelScope.launch {
                for (gameId in probeOrder) {
                    ensureActive()
                    if (loadEpoch != catalogLoadEpoch) return@launch
                    val probedInstallState = gameCatalogRepository.getInstallState(gameId)
                    ensureActive()
                    if (loadEpoch != catalogLoadEpoch) return@launch
                    publishInstallState(gameId, probedInstallState)
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
                AppLog.i("Catalog", "Starting launcher update download")
                launcherUpdateRepository
                    .downloadAndApplyUpdate()
                    .onSuccess {
                        AppLog.i("Catalog", "Launcher update handoff complete")
                    }.onFailure { error ->
                        AppLog.e("Catalog", "Launcher update failed", error)
                        updateState {
                            copy(
                                updateErrorMessage = error.message ?: "Update failed",
                                statusLabel =
                                    if (isUpdateGateActive) {
                                        "LAUNCHER · UPDATE REQUIRED"
                                    } else {
                                        statusLabel
                                    },
                            )
                        }
                    }
            } finally {
                updateState { copy(isUpdateDownloading = false, isLauncherUpdateSheetVisible = false) }
            }
        }
    }

    private fun selectGame(gameId: String) {
        updateState {
            copy(
                selectedGameId = gameId,
                selectedVersion = null,
                versionHistory = emptyList(),
                isVersionPickerVisible = false,
                isVersionHistoryLoading = false,
                installState = InstallState.Unknown,
                // Preserve in-flight ops for the game being selected; never clear the private
                // active* ids here — that is what allowed re-clicking DOWNLOAD mid-install.
                isDownloading = gameId == activeDownloadGameId,
                isChargingUninstall = false,
                isUninstalling = gameId == activeUninstallGameId,
                onDiskSizeBytes = null,
                ambientColor = Color.Transparent,
                launchErrorMessage = null,
            )
        }
        probeInstallState(gameId)
    }

    private fun moveSelection(delta: Int) {
        val games = state.value.games
        if (games.isEmpty()) return

        val currentIndex =
            games.indexOfFirst { it.id == state.value.selectedGameId }.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + delta).coerceIn(0, games.lastIndex)
        selectGame(games[nextIndex].id)
    }

    private fun toggleVersionPicker() {
        val game = state.value.selectedGame ?: return
        val willExpand = !state.value.isVersionPickerVisible

        updateState {
            copy(isVersionPickerVisible = willExpand)
        }

        if (willExpand && state.value.versionHistory.isEmpty()) {
            loadVersionHistory(game.versionsUrl, game.versionHistory)
        }
    }

    private fun loadVersionHistory(
        versionsUrl: String,
        embeddedHistory: List<com.morphingcoffee.gamelauncher.core.model.GameVersionEntry>,
    ) {
        if (embeddedHistory.isNotEmpty()) {
            if (state.value.selectedGame?.versionsUrl == versionsUrl) {
                updateState { copy(versionHistory = embeddedHistory) }
            }
            return
        }

        viewModelScope.launch {
            updateState { copy(isVersionHistoryLoading = true) }

            gameCatalogRepository
                .fetchVersionHistory(versionsUrl)
                .onSuccess { versions ->
                    if (state.value.selectedGame?.versionsUrl != versionsUrl) return@launch
                    updateState {
                        copy(
                            versionHistory = versions,
                            isVersionHistoryLoading = false,
                        )
                    }
                }.onFailure { error ->
                    if (state.value.selectedGame?.versionsUrl != versionsUrl) return@launch
                    updateState {
                        copy(
                            isVersionHistoryLoading = false,
                            launchErrorMessage = error.message,
                            statusLabel = "ERROR",
                        )
                    }
                }
        }
    }

    private fun selectVersion(version: String) {
        updateState {
            copy(
                selectedVersion = version,
                launchErrorMessage = null,
            )
        }
        state.value.selectedGameId?.let { probeInstallState(it) }
    }

    private fun probeInstallState(gameId: String) {
        viewModelScope.launch {
            val probedInstallState = gameCatalogRepository.getInstallState(gameId)
            ensureActive()
            publishInstallState(gameId, probedInstallState)
        }
    }

    private fun markInstalledAfterDownload(
        gameId: String,
        version: String,
        executablePath: String,
    ) {
        publishInstallState(
            gameId,
            InstallState.Installed(
                version = version,
                executablePath = executablePath,
            ),
        )
    }

    private fun publishInstallState(
        gameId: String,
        probedInstallState: InstallState,
    ) {
        val isSelected = state.value.selectedGameId == gameId
        updateState {
            val withCache =
                copy(installStatesByGameId = installStatesByGameId + (gameId to probedInstallState))
            if (!isSelected) {
                withCache
            } else {
                withCache.copy(installState = probedInstallState)
            }
        }
        if (!isSelected) return
        if (probedInstallState is InstallState.Installed &&
            state.value.displayVersion == probedInstallState.version
        ) {
            probeOnDiskSize(gameId)
        } else {
            updateState { copy(onDiskSizeBytes = null) }
        }
    }

    private fun probeOnDiskSize(gameId: String) {
        viewModelScope.launch {
            val onDiskSizeBytes = gameCatalogRepository.getOnDiskSizeBytes(gameId)
            if (state.value.selectedGameId != gameId) return@launch
            if (!state.value.isInstalledForDisplay) return@launch
            updateState { copy(onDiskSizeBytes = onDiskSizeBytes) }
        }
    }

    private fun downloadSelectedVersion() {
        val game = state.value.selectedGame ?: return
        val version =
            if (state.value.gameUpdateAvailable) {
                game.latestVersion
            } else {
                state.value.displayVersion
            }
        val build = resolveBuildForVersion(game, version) ?: return
        if (version.isBlank()) return
        // Single active download (shared progress stream + shared staging/game dirs).
        if (activeDownloadGameId != null || activeUninstallGameId != null) return

        val gameId = game.id
        val versionAtStart = version
        activeDownloadGameId = gameId
        updateState {
            copy(
                isDownloading = true,
                launchErrorMessage = null,
                statusLabel = if (gameUpdateAvailable) "GAME · UPDATING" else "GAME · DOWNLOADING",
                selectedVersion = if (gameUpdateAvailable) game.latestVersion else selectedVersion,
            )
        }

        viewModelScope.launch {
            try {
                AppLog.i("Catalog", "Downloading $gameId version $versionAtStart")
                val result =
                    gameCatalogRepository.downloadAndInstall(
                        gameId = gameId,
                        version = versionAtStart,
                        build = build,
                    )
                result
                    .onSuccess {
                        AppLog.i("Catalog", "Install complete for $gameId version $versionAtStart")
                        markInstalledAfterDownload(
                            gameId = gameId,
                            version = versionAtStart,
                            executablePath = build.executablePath,
                        )
                        if (state.value.selectedGameId == gameId) {
                            updateState {
                                copy(
                                    statusLabel = "READY",
                                    launchErrorMessage = null,
                                )
                            }
                        }
                    }.onFailure { error ->
                        AppLog.e("Catalog", "Install failed for $gameId version $versionAtStart", error)
                        if (state.value.selectedGameId == gameId &&
                            state.value.displayVersion == versionAtStart
                        ) {
                            updateState {
                                copy(
                                    statusLabel = "ERROR",
                                    launchErrorMessage = error.message,
                                )
                            }
                        }
                    }
            } finally {
                if (activeDownloadGameId == gameId) {
                    activeDownloadGameId = null
                }
                if (state.value.selectedGameId == gameId) {
                    updateState { copy(isDownloading = false) }
                }
            }
        }
    }

    private fun launchSelectedGame() {
        val game = state.value.selectedGame ?: return
        viewModelScope.launch {
            AppLog.i("Catalog", "Launching ${game.id}")
            updateState {
                copy(
                    statusLabel = "LAUNCHING",
                    isChargingLaunch = false,
                    isLaunching = true,
                    launchErrorMessage = null,
                    contentAlpha = 0f,
                )
            }

            gameCatalogRepository
                .launchGame(game.id, game.title)
                .onSuccess {
                    AppLog.i("Catalog", "Launch finished for ${game.id}")
                    updateState {
                        copy(
                            statusLabel = "READY",
                            isLaunching = false,
                            contentAlpha = 1f,
                        )
                    }
                }.onFailure { error ->
                    if (error is SimulatedLaunchException) {
                        updateState {
                            copy(
                                statusLabel = "LAUNCHED (DEV)",
                                launchErrorMessage = null,
                                isChargingLaunch = false,
                                isLaunching = false,
                                contentAlpha = 1f,
                            )
                        }
                        return@launch
                    }

                    updateState {
                        copy(
                            statusLabel = "ERROR",
                            launchErrorMessage = error.message,
                            isChargingLaunch = false,
                            isLaunching = false,
                            contentAlpha = 1f,
                        )
                    }
                    AppLog.e("Catalog", "Launch failed for ${game.id}", error)
                }
        }
    }

    private fun openWebGame() {
        val game = state.value.selectedGame ?: return
        val build = state.value.displayBuild ?: return
        if (!state.value.isWebGame) return

        viewModelScope.launch {
            try {
                AppLog.i("Catalog", "Opening web game ${game.id}")
                updateState {
                    copy(
                        statusLabel = "OPENING",
                        launchErrorMessage = null,
                    )
                }

                gameCatalogRepository
                    .openWebGame(build.downloadUrl)
                    .onSuccess {
                        AppLog.i("Catalog", "Browser opened for ${game.id}")
                        updateState {
                            copy(statusLabel = "READY")
                        }
                    }.onFailure { error ->
                        AppLog.e("Catalog", "Failed to open web game ${game.id}", error)
                        updateState {
                            copy(
                                statusLabel = "ERROR",
                                launchErrorMessage =
                                    error.message ?: "Failed to open browser. See F12 logs for details.",
                            )
                        }
                    }
            } catch (e: Throwable) {
                ensureActive()
                AppLog.e("Catalog", "Failed to open web game ${game.id}", e)
                updateState {
                    copy(
                        statusLabel = "ERROR",
                        launchErrorMessage =
                            e.message ?: "Failed to open browser. See F12 logs for details.",
                    )
                }
            }
        }
    }

    private fun uninstallSelectedGame() {
        val game = state.value.selectedGame ?: return
        if (state.value.isLaunching ||
            activeDownloadGameId != null ||
            activeUninstallGameId != null
        ) {
            return
        }

        val gameId = game.id
        val versionAtStart = state.value.displayVersion

        activeUninstallGameId = gameId
        updateState {
            copy(
                isChargingUninstall = false,
                isUninstalling = true,
                statusLabel = "UNINSTALLING",
                launchErrorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                AppLog.i("Catalog", "Uninstalling $gameId version $versionAtStart")

                val result = gameCatalogRepository.uninstallGame(gameId)

                if (state.value.selectedGameId != gameId || state.value.displayVersion != versionAtStart) {
                    return@launch
                }

                result
                    .onSuccess {
                        AppLog.i("Catalog", "Uninstall complete for $gameId")
                        updateState {
                            copy(
                                installState = InstallState.NotInstalled,
                                installStatesByGameId = installStatesByGameId + (gameId to InstallState.NotInstalled),
                                onDiskSizeBytes = null,
                                statusLabel = "READY",
                                launchErrorMessage = null,
                            )
                        }
                    }.onFailure { error ->
                        AppLog.e("Catalog", "Uninstall failed for $gameId", error)
                        probeInstallState(gameId)
                        updateState {
                            copy(
                                statusLabel = "ERROR",
                                launchErrorMessage =
                                    error.message ?: "Uninstall failed. See F12 logs for details.",
                            )
                        }
                    }
            } finally {
                if (activeUninstallGameId == gameId) {
                    activeUninstallGameId = null
                }
                if (state.value.selectedGameId == gameId) {
                    updateState { copy(isUninstalling = false) }
                }
            }
        }
    }

    private fun resolveBuildForVersion(
        game: GameCatalogEntry,
        version: String,
    ): com.morphingcoffee.gamelauncher.core.model.GameBuild? {
        state.value.versionHistory
            .firstOrNull { it.version == version }
            ?.buildForCurrentPlatform()
            ?.let { return it }

        return if (version == game.latestVersion) {
            game.buildForCurrentPlatform()
        } else {
            null
        }
    }
}
