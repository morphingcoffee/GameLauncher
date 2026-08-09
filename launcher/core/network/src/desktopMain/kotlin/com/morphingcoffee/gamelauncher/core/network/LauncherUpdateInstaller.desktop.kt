package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.telemetry.CrashReporting
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val READ_BUFFER_SIZE = 256 * 1024

internal class LauncherUpdateInstallerImpl(
    private val downloadHttpClient: HttpClient,
    private val libraryLayout: LibraryLayout = LibraryPaths,
    private val processLauncher: ProcessLauncher = RealProcessLauncher,
    private val desktopActions: DesktopActions = RealDesktopActions,
    private val processExiter: ProcessExiter = RealProcessExiter,
    private val osName: () -> String = { System.getProperty("os.name") },
    private val handoffDelayMillis: Long = 500L,
) : LauncherUpdateInstaller {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    override suspend fun downloadAndApply(
        channelBuild: LauncherChannelBuild,
        versionLabel: String,
    ): Result<Unit> =
        runCatching {
            if (channelBuild.fileSizeBytes <= 0L) {
                error("Update file size must be greater than zero")
            }

            val extension = extensionForArtifact(channelBuild.artifactType)
            val destination =
                File(
                    libraryLayout.userDownloadsDirectory(),
                    "GameLauncher-$versionLabel$extension",
                )
            destination.parentFile?.mkdirs()

            val stagingFile = File(libraryLayout.launcherUpdatesDirectory(), "update-$versionLabel$extension.part")
            stagingFile.parentFile?.mkdirs()

            var resumeOffset = 0L
            if (stagingFile.exists()) {
                resumeOffset = stagingFile.length()
            }

            try {
                withContext(Dispatchers.IO) {
                    downloadToStaging(
                        channelBuild = channelBuild,
                        stagingFile = stagingFile,
                        resumeOffset = resumeOffset,
                    )
                }

                withContext(Dispatchers.Default) {
                    AppLog.i("LauncherUpdate", "Verifying update download for $versionLabel")
                    verifySha256(stagingFile, channelBuild.sha256)
                }

                withContext(Dispatchers.IO) {
                    stagingFile.copyTo(destination, overwrite = true)
                }

                withContext(Dispatchers.Main) {
                    applyUpdate(
                        artifactType = channelBuild.artifactType,
                        destination = destination,
                    )
                }
            } finally {
                withContext(NonCancellable + Dispatchers.IO) {
                    if (stagingFile.exists()) {
                        stagingFile.delete()
                    }
                }
                _downloadProgress.value = null
            }
        }.onFailure { error ->
            AppLog.e("LauncherUpdate", "Update failed for $versionLabel", error)
            CrashReporting.captureUpdateFailure(versionLabel, error)
            _downloadProgress.value = null
        }

    private suspend fun downloadToStaging(
        channelBuild: LauncherChannelBuild,
        stagingFile: File,
        resumeOffset: Long,
    ) {
        val totalBytes = channelBuild.fileSizeBytes

        downloadHttpClient
            .prepareGet(channelBuild.downloadUrl) {
                if (resumeOffset > 0L) {
                    header(HttpHeaders.Range, "bytes=$resumeOffset-")
                }
            }.execute { response ->
                val appendMode =
                    when (response.status) {
                        HttpStatusCode.PartialContent -> resumeOffset > 0L
                        HttpStatusCode.OK -> {
                            if (resumeOffset > 0L) {
                                stagingFile.delete()
                            }
                            false
                        }
                        else -> error("Update download failed: ${response.status}")
                    }

                var bytesDownloaded = if (appendMode) resumeOffset else 0L
                _downloadProgress.value =
                    DownloadProgress(
                        gameId = LAUNCHER_UPDATE_PROGRESS_ID,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                    )

                val buffer = ByteArray(READ_BUFFER_SIZE)
                val channel = response.bodyAsChannel()

                FileOutputStream(stagingFile, appendMode).use { output ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        _downloadProgress.value =
                            DownloadProgress(
                                gameId = LAUNCHER_UPDATE_PROGRESS_ID,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                            )
                    }
                }
            }

        val finalSize = stagingFile.length()
        if (finalSize != channelBuild.fileSizeBytes) {
            stagingFile.delete()
            error("Downloaded size $finalSize does not match expected ${channelBuild.fileSizeBytes}")
        }
    }

    private fun verifySha256(
        file: File,
        expectedSha256: String,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(READ_BUFFER_SIZE)

        file.inputStream().use { input ->
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }

        val actualHash = digest.digest().toHexString()
        if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
            file.delete()
            error("SHA-256 mismatch: expected $expectedSha256, got $actualHash")
        }
    }

    private suspend fun applyUpdate(
        artifactType: String,
        destination: File,
    ) {
        when (artifactType.lowercase()) {
            "msi" -> {
                processLauncher.start(
                    command = listOf("msiexec", "/i", destination.absolutePath, "/passive"),
                    workingDirectory = null,
                    redirectErrorStream = false,
                )
                delay(handoffDelayMillis)
                CrashReporting.flush()
                processExiter.exit(0)
            }
            "dmg" -> {
                if (!desktopActions.isDesktopSupported()) {
                    error("Desktop API is not supported on this platform")
                }
                desktopActions.open(destination)
                delay(handoffDelayMillis)
                CrashReporting.flush()
                processExiter.exit(0)
            }
            "zip" -> {
                revealDownloadedFile(destination)
            }
            else -> {
                if (desktopActions.isDesktopSupported()) {
                    desktopActions.open(destination)
                } else {
                    error("Unsupported update artifact type: $artifactType")
                }
            }
        }
    }

    private fun revealDownloadedFile(destination: File) {
        val os = osName().lowercase()
        when {
            "win" in os -> {
                processLauncher.start(
                    command = listOf("explorer.exe", "/select,${destination.absolutePath}"),
                    workingDirectory = null,
                    redirectErrorStream = false,
                )
            }
            "mac" in os || "darwin" in os -> {
                processLauncher.start(
                    command = listOf("open", "-R", destination.absolutePath),
                    workingDirectory = null,
                    redirectErrorStream = false,
                )
            }
            desktopActions.isDesktopSupported() -> {
                val parent = destination.parentFile
                if (parent != null) {
                    desktopActions.openDirectory(parent)
                } else {
                    desktopActions.open(destination)
                }
            }
            else -> error("Desktop API is not supported on this platform")
        }
    }

    private fun extensionForArtifact(artifactType: String): String =
        when (artifactType.lowercase()) {
            "msi" -> ".msi"
            "dmg" -> ".dmg"
            "zip" -> ".zip"
            else -> ".bin"
        }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

actual fun createLauncherUpdateInstaller(downloadHttpClient: HttpClient): LauncherUpdateInstaller =
    LauncherUpdateInstallerImpl(downloadHttpClient)
