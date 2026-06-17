package com.morphingcoffee.gamelauncher.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogFormattersTest {
    @Test
    fun formatSizeDisplay_webGame_returnsBrowser() {
        assertEquals(
            "BROWSER",
            formatSizeDisplay(
                downloadSizeBytes = 100L,
                uncompressedSizeBytes = 200L,
                onDiskSizeBytes = null,
                isWebGame = true,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_preDownload_usesUncompressedSize() {
        assertEquals(
            "95 MB",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = 100_000_000L,
                onDiskSizeBytes = null,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_preDownload_fallsBackToDownloadSize() {
        assertEquals(
            "48 MB",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = null,
                onDiskSizeBytes = null,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_installed_showsOnDiskOnly() {
        assertEquals(
            "114 MB ON DISK",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = 100_000_000L,
                onDiskSizeBytes = 120_000_000L,
                isInstalled = true,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_installedWithoutProbe_returnsNotAvailable() {
        assertEquals(
            "NOT AVAILABLE",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = 100_000_000L,
                onDiskSizeBytes = null,
                isInstalled = true,
            ),
        )
    }
}
