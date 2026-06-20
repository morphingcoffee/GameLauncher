package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThumbnailDiskCacheTest {
    @Test
    fun thumbnailDiskContentChanged_detectsEtagChange() {
        assertTrue(
            thumbnailDiskContentChanged(
                hadDiskEntry = true,
                baselineEtag = "\"old\"",
                freshEtag = "\"new\"",
            ),
        )
    }

    @Test
    fun thumbnailDiskContentChanged_ignoresMissingDiskEntry() {
        assertFalse(
            thumbnailDiskContentChanged(
                hadDiskEntry = false,
                baselineEtag = "\"old\"",
                freshEtag = "\"new\"",
            ),
        )
    }

    @Test
    fun thumbnailDiskContentChanged_ignoresMatchingEtag() {
        assertFalse(
            thumbnailDiskContentChanged(
                hadDiskEntry = true,
                baselineEtag = "\"same\"",
                freshEtag = "\"same\"",
            ),
        )
    }

    @Test
    fun thumbnailDiskContentChanged_ignoresMissingEtagMetadata() {
        assertFalse(
            thumbnailDiskContentChanged(
                hadDiskEntry = true,
                baselineEtag = null,
                freshEtag = "\"new\"",
            ),
        )
    }
}
