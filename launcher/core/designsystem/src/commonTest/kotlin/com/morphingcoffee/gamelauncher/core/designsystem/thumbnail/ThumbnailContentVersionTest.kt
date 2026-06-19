package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Image
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThumbnailContentVersionTest {
    @Test
    fun thumbnailContentChanged_detectsEtagChange() {
        assertTrue(
            thumbnailContentChanged(
                baselineEtag = "\"old\"",
                freshEtag = "\"new\"",
                displayedContentHash = "abc",
                validationImage = StubImage,
            ),
        )
    }

    @Test
    fun thumbnailContentChanged_ignoresMatchingEtagWithoutHashChange() {
        assertFalse(
            thumbnailContentChanged(
                baselineEtag = "\"same\"",
                freshEtag = "\"same\"",
                displayedContentHash = null,
                validationImage = StubImage,
            ),
        )
    }

    private object StubImage : Image {
        override val size: Long = 0L
        override val width: Int = 1
        override val height: Int = 1
        override val shareable: Boolean = true

        override fun draw(canvas: coil3.Canvas) = Unit
    }
}
