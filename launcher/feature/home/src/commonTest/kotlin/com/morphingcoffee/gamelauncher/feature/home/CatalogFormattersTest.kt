package com.morphingcoffee.gamelauncher.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogFormattersTest {
    @Test
    fun formatSizeLabel_webGame_isHidden() {
        assertNull(
            formatSizeLabel(
                isWebGame = true,
                isInstalled = false,
                isDownloading = false,
                uncompressedSizeBytes = 100L,
                downloadSizeBytes = 50L,
            ),
        )
    }

    @Test
    fun formatSizeLabel_preDownload_usesSizeWhenBothKnown() {
        assertEquals(
            "SIZE",
            formatSizeLabel(
                isWebGame = false,
                isInstalled = false,
                isDownloading = false,
                uncompressedSizeBytes = 100_000_000L,
                downloadSizeBytes = 50_000_000L,
            ),
        )
    }

    @Test
    fun formatSizeLabel_preDownload_usesDownloadWhenUncompressedMissing() {
        assertEquals(
            "DOWNLOAD",
            formatSizeLabel(
                isWebGame = false,
                isInstalled = false,
                isDownloading = false,
                uncompressedSizeBytes = null,
                downloadSizeBytes = 50_000_000L,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_preDownload_showsDownloadAndInstallSize() {
        assertEquals(
            "48 MB DL / 95 MB ON DISK",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = 100_000_000L,
                onDiskSizeBytes = null,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_preDownload_fallsBackToDownloadSizeWithDlSuffix() {
        assertEquals(
            "48 MB DL",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = null,
                onDiskSizeBytes = null,
            ),
        )
    }

    @Test
    fun formatSizeDisplay_downloading_showsDownloadAndInstallProgress() {
        assertEquals(
            "48 MB DL / 95 MB INSTALLING",
            formatSizeDisplay(
                downloadSizeBytes = 50_000_000L,
                uncompressedSizeBytes = 100_000_000L,
                onDiskSizeBytes = null,
                isDownloading = true,
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
