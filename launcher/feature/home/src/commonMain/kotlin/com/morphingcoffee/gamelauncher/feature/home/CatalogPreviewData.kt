package com.morphingcoffee.gamelauncher.feature.home

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry

fun catalogPreviewState(platformKey: String = "macos-arm64"): CatalogState =
    CatalogState(
        isLoading = false,
        games =
            listOf(
                previewGame(id = "alpha", title = "Alpha Build", platformKey = platformKey),
                previewGame(
                    id = "beta",
                    title = "Beta Showcase",
                    available = false,
                    platformKey = platformKey,
                ),
            ),
        selectedGameId = "alpha",
        clockText = "13:42:01",
        platformKey = platformKey,
    )

private fun previewGame(
    id: String,
    title: String,
    available: Boolean = true,
    platformKey: String = "macos-arm64",
): GameCatalogEntry =
    GameCatalogEntry(
        id = id,
        title = title,
        description = "Preview entry",
        thumbnailUrl = "https://example.com/$id.webp",
        latestVersion = "0.0.1",
        versionsUrl = "https://example.com/$id/versions.json",
        builds =
            if (available) {
                mapOf(
                    platformKey to
                        GameBuild(
                            downloadUrl = "https://example.com/$id.zip",
                            executablePath = previewExecutablePath(platformKey),
                            fileSizeBytes = 48_234_567L,
                            uncompressedSizeBytes = 96_500_000L,
                            sha256 = "abc123",
                        ),
                )
            } else {
                mapOf(
                    "windows-x64" to
                        GameBuild(
                            downloadUrl = "https://example.com/$id.zip",
                            executablePath = "Game.exe",
                            fileSizeBytes = 48_234_567L,
                            uncompressedSizeBytes = 96_500_000L,
                            sha256 = "abc123",
                        ),
                )
            },
    )

private fun previewExecutablePath(platformKey: String): String =
    when (platformKey) {
        "windows-x64" -> "Game.exe"
        else -> "Game.app/Contents/MacOS/Game"
    }
