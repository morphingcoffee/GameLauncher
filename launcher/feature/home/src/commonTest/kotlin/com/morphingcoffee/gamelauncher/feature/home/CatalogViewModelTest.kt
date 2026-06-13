package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.network.GameCatalogRepository
import com.morphingcoffee.gamelauncher.core.network.ManifestRepository
import com.morphingcoffee.gamelauncher.core.network.createGameLauncher
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CatalogViewModelTest {
    @Test
    fun started_loadsCatalogAndSelectsFirstGame() =
        runBlocking {
            val repository = GameCatalogRepository(createManifestRepository(sampleManifestJson()), createGameLauncher())
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("READY", state.statusLabel)
            assertEquals(2, state.games.size)
            assertEquals("alpha", state.selectedGameId)
            assertNull(state.errorMessage)
        }

    @Test
    fun moveSelection_updatesSelectedGame() =
        runBlocking {
            val repository = GameCatalogRepository(createManifestRepository(sampleManifestJson()), createGameLauncher())
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(50)

            assertEquals("beta", viewModel.state.value.selectedGameId)
        }

    @Test
    fun loadFailure_setsErrorState() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine {
                        respond(
                            content = "not-json",
                            status = HttpStatusCode.InternalServerError,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
            val repository = GameCatalogRepository(ManifestRepository(client), createGameLauncher())
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("ERROR", state.statusLabel)
            assertEquals(null, state.selectedGameId)
        }

    @Test
    fun gameSelection_clearsLaunchErrorAndAmbientColor() =
        runBlocking {
            val repository =
                GameCatalogRepository(
                    createManifestRepository(sampleManifestJson()),
                    createGameLauncher(),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(
                CatalogEvent.AmbientColorExtracted(
                    color = Color.Red,
                    imageUrl = "https://example.com/alpha.webp",
                ),
            )
            viewModel.onEvent(CatalogEvent.LaunchClicked)
            viewModel.onEvent(CatalogEvent.LaunchChargeComplete)
            delay(200)

            assertEquals("ERROR", viewModel.state.value.statusLabel)
            assertEquals("alpha", viewModel.state.value.selectedGameId)

            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(50)

            val state = viewModel.state.value
            assertEquals("beta", state.selectedGameId)
            assertNull(state.launchErrorMessage)
            assertEquals(Color.Transparent, state.ambientColor)
        }

    private suspend fun waitForLoadingToFinish(viewModel: CatalogViewModel) {
        repeat(100) {
            if (!viewModel.state.value.isLoading) return
            delay(25)
        }
        error("Catalog load did not complete in time: ${viewModel.state.value}")
    }

    private fun createManifestRepository(manifestJson: String): ManifestRepository {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        content = manifestJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        return ManifestRepository(client, manifestUrl = "https://example.com/manifest.json")
    }

    private fun sampleManifestJson(): String =
        """
        {
          "schema_version": 1,
          "launcher_minimum_version": "0.0.1",
          "games": [
            {
              "id": "alpha",
              "title": "Alpha Build",
              "description": "Preview",
              "thumbnail_url": "https://example.com/alpha.webp",
              "latest_version": "0.0.1",
              "versions_url": "https://example.com/alpha/versions.json",
              "builds": {
                "macos-arm64": {
                  "download_url": "https://example.com/alpha.zip",
                  "executable_path": "Game.app/Contents/MacOS/Game",
                  "file_size_bytes": 1024,
                  "sha256": "abc"
                }
              }
            },
            {
              "id": "beta",
              "title": "Beta Showcase",
              "description": "Preview",
              "thumbnail_url": "https://example.com/beta.webp",
              "latest_version": "0.0.1",
              "versions_url": "https://example.com/beta/versions.json",
              "builds": {
                "macos-arm64": {
                  "download_url": "https://example.com/beta.zip",
                  "executable_path": "Game.app/Contents/MacOS/Game",
                  "file_size_bytes": 1024,
                  "sha256": "abc"
                }
              }
            }
          ]
        }
        """.trimIndent()
}
