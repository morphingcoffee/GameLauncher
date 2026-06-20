package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.Manifest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestRepositoryTest {
    @Test
    fun fetchManifest_deserializesCatalog() =
        kotlinx.coroutines.test.runTest {
            val manifestJson =
                """
                {
                  "launcher_minimum_version": "0.0.1",
                  "games": [
                    {
                      "id": "cool_game",
                      "title": "Cool Game",
                      "description": "A short description.",
                      "thumbnail_url": "https://cdn.example.com/assets/cool_game/thumbnail.webp",
                      "latest_version": "1.2.0",
                      "versions_url": "https://cdn.example.com/games/cool_game/versions.json",
                      "builds": {
                        "macos-arm64": {
                          "download_url": "https://cdn.example.com/games/cool_game/v1.2.0/macos-arm64/game.zip",
                          "executable_path": "CoolGame.app/Contents/MacOS/CoolGame",
                          "file_size_bytes": 48234567,
                          "sha256": "abc123"
                        }
                      }
                    }
                  ]
                }
                """.trimIndent()

            val client =
                HttpClient(
                    MockEngine { request ->
                        respond(
                            content = manifestJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                            },
                        )
                    }
                }

            val repository = ManifestRepository(client, manifestUrl = "https://example.com/manifest.json")
            val manifest: Manifest = repository.fetchManifest()

            assertEquals(1, manifest.games.size)
            assertEquals("cool_game", manifest.games.first().id)
            assertEquals("1.2.0", manifest.games.first().latestVersion)
        }
}
