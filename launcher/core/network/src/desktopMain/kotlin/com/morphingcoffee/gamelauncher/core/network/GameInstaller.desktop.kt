package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

private const val READ_BUFFER_SIZE = 256 * 1024

actual class GameInstaller(
    private val downloadHttpClient: HttpClient,
) {
    actual suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<Unit> =
        runCatching {
            if (build.fileSizeBytes <= 0L) {
                error("Build file size must be greater than zero")
            }

            val stagingFile = File(LibraryPaths.downloadStagingFile(gameId, version))
            stagingFile.parentFile?.mkdirs()

            var resumeOffset = 0L
            if (stagingFile.exists()) {
                resumeOffset = stagingFile.length()
            }

            try {
                downloadToStaging(
                    build = build,
                    gameId = gameId,
                    stagingFile = stagingFile,
                    resumeOffset = resumeOffset,
                    onProgress = onProgress,
                )

                verifySha256(stagingFile, build.sha256)

                val gameDir = File(LibraryPaths.gameDirectory(gameId))
                if (gameDir.exists()) {
                    gameDir.deleteRecursively()
                }
                gameDir.mkdirs()

                extractZip(stagingFile, gameDir)

                val executable = File(gameDir, build.executablePath)
                if (!executable.exists()) {
                    gameDir.deleteRecursively()
                    error("Executable not found after extract: ${build.executablePath}")
                }

                writeInstallRecord(
                    gameId = gameId,
                    version = version,
                    executablePath = build.executablePath,
                    sha256 = build.sha256,
                )
            } finally {
                if (stagingFile.exists()) {
                    stagingFile.delete()
                }
            }
        }

    actual fun getInstallState(gameId: String): InstallState {
        val recordFile = File(LibraryPaths.installRecordFile(gameId))
        if (!recordFile.exists()) {
            return InstallState.NotInstalled
        }

        return runCatching {
            val record = Json.decodeFromString<GameInstallRecord>(recordFile.readText())
            val executable = File(LibraryPaths.gameDirectory(gameId), record.executablePath)
            if (!executable.exists()) {
                InstallState.NotInstalled
            } else {
                InstallState.Installed(
                    version = record.version,
                    executablePath = record.executablePath,
                )
            }
        }.getOrElse {
            InstallState.NotInstalled
        }
    }

    actual suspend fun uninstall(gameId: String): Result<Unit> =
        runCatching {
            val gameDir = File(LibraryPaths.gameDirectory(gameId))
            if (!gameDir.exists()) {
                return@runCatching
            }

            if (!gameDir.deleteRecursively()) {
                error("Could not remove all game files. Close the game if it is running and try again.")
            }

            if (gameDir.exists()) {
                error("Game files could not be fully removed. Close the game if it is running and try again.")
            }
        }

    actual fun getOnDiskSizeBytes(gameId: String): Long? {
        val gameDir = File(LibraryPaths.gameDirectory(gameId))
        if (!gameDir.exists()) {
            return null
        }

        val recordFile = File(LibraryPaths.installRecordFile(gameId))
        if (!recordFile.exists()) {
            return null
        }

        return gameDir
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private suspend fun downloadToStaging(
        build: GameBuild,
        gameId: String,
        stagingFile: File,
        resumeOffset: Long,
        onProgress: (DownloadProgress) -> Unit,
    ) {
        val totalBytes = build.fileSizeBytes

        downloadHttpClient
            .prepareGet(build.downloadUrl) {
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
                        else -> error("Download failed: ${response.status}")
                    }

                var bytesDownloaded = if (appendMode) resumeOffset else 0L
                onProgress(DownloadProgress(gameId, bytesDownloaded, totalBytes))

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
                        onProgress(DownloadProgress(gameId, bytesDownloaded, totalBytes))
                    }
                }
            }

        val finalSize = stagingFile.length()
        if (finalSize != build.fileSizeBytes) {
            stagingFile.delete()
            error("Downloaded size $finalSize does not match expected ${build.fileSizeBytes}")
        }
    }

    private fun verifySha256(
        stagingFile: File,
        expectedSha256: String,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(READ_BUFFER_SIZE)

        stagingFile.inputStream().use { input ->
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }

        val actualHash = digest.digest().toHexString()
        if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
            stagingFile.delete()
            error("SHA-256 mismatch: expected $expectedSha256, got $actualHash")
        }
    }

    private fun extractZip(
        stagingFile: File,
        gameDir: File,
    ) {
        ZipInputStream(stagingFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val destFile = File(gameDir, entry.name)
                val canonicalDest = destFile.canonicalPath
                val canonicalGameDir = gameDir.canonicalPath
                val isInsideGameDir =
                    canonicalDest == canonicalGameDir ||
                        canonicalDest.startsWith("$canonicalGameDir${File.separator}")

                if (!isInsideGameDir) {
                    throw SecurityException("Zip entry escapes game directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    destFile.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun writeInstallRecord(
        gameId: String,
        version: String,
        executablePath: String,
        sha256: String,
    ) {
        val record =
            GameInstallRecord(
                gameId = gameId,
                version = version,
                executablePath = executablePath,
                sha256 = sha256,
            )
        val recordFile = File(LibraryPaths.installRecordFile(gameId))
        recordFile.parentFile?.mkdirs()
        recordFile.writeText(Json.encodeToString(record))
    }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

actual fun createGameInstaller(downloadHttpClient: HttpClient): GameInstaller = GameInstaller(downloadHttpClient)
