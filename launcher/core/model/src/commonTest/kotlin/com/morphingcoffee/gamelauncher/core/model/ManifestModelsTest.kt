package com.morphingcoffee.gamelauncher.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManifestModelsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun catalogManifest_deserializes() {
        val raw =
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
                      "uncompressed_size_bytes": 96500000,
                      "sha256": "abc123"
                    }
                  }
                }
              ]
            }
            """.trimIndent()

        val manifest = json.decodeFromString<Manifest>(raw)

        assertEquals(1, manifest.games.size)
        assertEquals("cool_game", manifest.games.first().id)
        assertEquals("1.2.0", manifest.games.first().latestVersion)
        assertNotNull(manifest.games.first().builds["macos-arm64"])
        assertEquals(
            96_500_000L,
            manifest.games
                .first()
                .builds["macos-arm64"]
                ?.uncompressedSizeBytes,
        )
    }

    @Test
    fun catalogManifest_deserializesWithMissingThumbnail() {
        val raw =
            """
            {
              "launcher_minimum_version": "0.0.1",
              "games": [
                {
                  "id": "text_only",
                  "title": "Text Only",
                  "description": "No artwork.",
                  "latest_version": "1.0.0",
                  "versions_url": "https://cdn.example.com/games/text_only/versions.json",
                  "builds": {}
                }
              ]
            }
            """.trimIndent()

        val manifest = json.decodeFromString<Manifest>(raw)

        assertNull(manifest.games.single().thumbnailUrl)
    }

    @Test
    fun versionIndex_deserializes() {
        val raw =
            """
            {
              "game_id": "cool_game",
              "versions": [
                {
                  "version": "1.0.0",
                  "released_at": "2026-05-01",
                  "builds": {
                    "windows-x64": {
                      "download_url": "https://cdn.example.com/games/cool_game/v1.0.0/windows-x64/game.zip",
                      "executable_path": "CoolGame.exe",
                      "file_size_bytes": 50000000,
                      "sha256": "def456"
                    }
                  }
                }
              ]
            }
            """.trimIndent()

        val index = json.decodeFromString<GameVersionIndex>(raw)

        assertEquals("cool_game", index.gameId)
        assertEquals("1.0.0", index.versions.single().version)
    }
}

class PlatformKeyTest {
    @Test
    fun all_containsExpectedKeys() {
        assertTrue(PlatformKey.WINDOWS_X64 in PlatformKey.all)
        assertTrue(PlatformKey.MACOS_ARM64 in PlatformKey.all)
        assertTrue(PlatformKey.MACOS_X64 in PlatformKey.all)
        assertTrue(PlatformKey.WEB in PlatformKey.all)
    }

    @Test
    fun current_returnsKeyOrNull() {
        val key = PlatformKey.current()
        if (key != null) {
            assertTrue(key in PlatformKey.all)
        } else {
            assertNull(key)
        }
    }
}

class WebGameBuildTest {
    private val webBuild =
        GameBuild(
            downloadUrl = "https://example.com/game/",
            executablePath = "",
            fileSizeBytes = 0,
            sha256 = "",
        )

    @Test
    fun webBuild_isAvailableOnAnyPlatform() {
        val entry =
            GameCatalogEntry(
                id = "web-game",
                title = "Web Game",
                description = "Browser game",
                latestVersion = "1.0.0",
                versionsUrl = "https://example.com/web-game/versions.json",
                builds = mapOf(PlatformKey.WEB to webBuild),
            )

        assertTrue(entry.isWebGame())
        assertTrue(entry.isAvailableOnCurrentPlatform())
        assertEquals(webBuild, entry.buildForCurrentPlatform())
    }

    @Test
    fun webBuild_deserializesFromManifest() {
        val raw =
            """
            {
              "launcher_minimum_version": "0.0.1",
              "games": [
                {
                  "id": "game_gallery",
                  "title": "Game Gallery",
                  "description": "Browser prototypes.",
                  "latest_version": "1.0.0",
                  "versions_url": "https://cdn.example.com/games/game_gallery/versions.json",
                  "builds": {
                    "web": {
                      "download_url": "https://morphingcoffee.github.io/apps/games/",
                      "executable_path": "",
                      "file_size_bytes": 0,
                      "sha256": ""
                    }
                  }
                }
              ]
            }
            """.trimIndent()

        val manifest = Json { ignoreUnknownKeys = true }.decodeFromString<Manifest>(raw)
        val game = manifest.games.single()

        assertEquals("game_gallery", game.id)
        assertTrue(game.isWebGame())
        assertEquals(
            "https://morphingcoffee.github.io/apps/games/",
            game.buildForCurrentPlatform()?.downloadUrl,
        )
    }
}
