package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class RecordedProcessStart(
    val command: List<String>,
    val workingDirectory: File?,
    val redirectErrorStream: Boolean,
)

internal class RecordingProcessLauncher(
    private val exitCode: Int = 0,
    private val processFactory: (RecordedProcessStart) -> Process = { StubProcess(exitCode) },
) : ProcessLauncher {
    val starts = mutableListOf<RecordedProcessStart>()

    override fun start(
        command: List<String>,
        workingDirectory: File?,
        redirectErrorStream: Boolean,
    ): Process {
        val recorded =
            RecordedProcessStart(
                command = command,
                workingDirectory = workingDirectory,
                redirectErrorStream = redirectErrorStream,
            )
        starts += recorded
        return processFactory(recorded)
    }
}

internal class RecordingProcessExiter : ProcessExiter {
    val exits = mutableListOf<Int>()

    override fun exit(status: Int) {
        exits += status
    }
}

internal class RecordingDesktopActions(
    private val desktopSupported: Boolean = true,
) : DesktopActions {
    val openedFiles = mutableListOf<File>()
    val openedDirectories = mutableListOf<File>()

    override fun isDesktopSupported(): Boolean = desktopSupported

    override fun open(file: File) {
        openedFiles += file
    }

    override fun openDirectory(directory: File) {
        openedDirectories += directory
    }
}

internal class StubProcess(
    private val code: Int = 0,
) : Process() {
    private val input = ByteArrayInputStream(ByteArray(0))
    private val output: OutputStream = ByteArrayOutputStream()
    private val error = ByteArrayInputStream(ByteArray(0))

    override fun getOutputStream(): OutputStream = output

    override fun getInputStream(): ByteArrayInputStream = input

    override fun getErrorStream(): ByteArrayInputStream = error

    override fun waitFor(): Int = code

    override fun exitValue(): Int = code

    override fun destroy() = Unit

    override fun isAlive(): Boolean = false
}

internal fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

internal fun zipBytes(entries: Map<String, ByteArray>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(content)
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}

internal fun gameBuild(
    bytes: ByteArray,
    executablePath: String = "Game.bin",
    downloadUrl: String = "https://cdn.example.com/game.zip",
): GameBuild =
    GameBuild(
        downloadUrl = downloadUrl,
        executablePath = executablePath,
        fileSizeBytes = bytes.size.toLong(),
        sha256 = sha256Hex(bytes),
    )

internal fun channelBuild(
    bytes: ByteArray,
    artifactType: String,
    downloadUrl: String = "https://cdn.example.com/update.bin",
    version: String = "1.0.0",
): LauncherChannelBuild =
    LauncherChannelBuild(
        version = version,
        artifactType = artifactType,
        downloadUrl = downloadUrl,
        fileSizeBytes = bytes.size.toLong(),
        sha256 = sha256Hex(bytes),
    )

internal fun mockDownloadClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): HttpClient = HttpClient(MockEngine(handler))

internal fun respondBytes(
    scope: MockRequestHandleScope,
    bytes: ByteArray,
    status: HttpStatusCode = HttpStatusCode.OK,
) = scope.respond(
    content = bytes,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
)
