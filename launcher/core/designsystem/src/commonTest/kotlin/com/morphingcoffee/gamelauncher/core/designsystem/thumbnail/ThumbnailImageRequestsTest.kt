package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.crossfadeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThumbnailImageRequestsTest {
    @Test
    fun buildThumbnailValidationRequest_usesRevalidatePolicies() {
        val request = buildThumbnailValidationRequest(PlatformContext.INSTANCE, IMAGE_URL)

        assertEquals(IMAGE_URL, request.data)
        assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.networkCachePolicy)
        assertTrue(request.thumbnailRevalidate)
        assertEquals(THUMBNAIL_CROSSFADE_MILLIS, request.crossfadeMillis)
    }

    private companion object {
        const val IMAGE_URL = "https://cdn.example.com/assets/cool_game/thumbnail.webp"
    }
}
