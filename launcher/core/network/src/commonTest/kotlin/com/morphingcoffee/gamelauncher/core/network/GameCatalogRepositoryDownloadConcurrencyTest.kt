package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GameCatalogRepositoryDownloadConcurrencyTest {
    @Test
    fun downloadAndInstall_rejectsSecondConcurrentCall() =
        runTest {
            val firstEntered = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val repository =
                GameCatalogRepository(
                    manifestRepository =
                        ManifestRepository(
                            HttpClient(
                                MockEngine {
                                    respond(
                                        content = """{"launcher_minimum_version":"0.0.1","games":[]}""",
                                        status = HttpStatusCode.OK,
                                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                    )
                                },
                            ),
                        ),
                    gameLauncher = createGameLauncher(),
                    gameInstaller = createGameInstaller(createDownloadHttpClient()),
                    downloadInstallRunner =
                        DownloadInstallRunner { _, _, _, _ ->
                            firstEntered.complete(Unit)
                            releaseFirst.await()
                            Result.success(Unit)
                        },
                )

            val build =
                GameBuild(
                    downloadUrl = "https://example.com/game.zip",
                    executablePath = "Game.bin",
                    fileSizeBytes = 10,
                    sha256 = "abc",
                )

            val first = async { repository.downloadAndInstall("alpha", "1.0.0", build) }
            firstEntered.await()

            val second = repository.downloadAndInstall("alpha", "1.0.0", build)
            assertTrue(second.isFailure)
            assertTrue(
                second.exceptionOrNull()?.message?.contains("already in progress") == true,
            )

            releaseFirst.complete(Unit)
            assertTrue(first.await().isSuccess)
        }
}
