package com.morphingcoffee.gamelauncher.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameCatalogRepositoryInstallStateTest {
    @Test
    fun getInstallState_executesBlockingProbeOnConfiguredIoDispatcher() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            var probeInvocations = 0
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
                    ioDispatcher = ioDispatcher,
                    installStateProbe =
                        BlockingInstallStateProbe {
                            probeInvocations++
                            InstallState.NotInstalled
                        },
                )

            val installState =
                withContext(kotlinx.coroutines.Dispatchers.Unconfined) {
                    repository.getInstallState("missing-game")
                }

            assertEquals(InstallState.NotInstalled, installState)
            assertEquals(1, probeInvocations)
            assertTrue(ioDispatcher.dispatchCount > 0, "install-state probe must hop to IO dispatcher")
        }

    private class RecordingDispatcher : CoroutineDispatcher() {
        @Volatile
        var dispatchCount: Int = 0

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount++
            block.run()
        }
    }
}
