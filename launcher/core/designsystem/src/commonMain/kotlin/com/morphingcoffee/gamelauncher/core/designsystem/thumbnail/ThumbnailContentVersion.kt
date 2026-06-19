package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Bitmap
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.InternalCoilApi
import coil3.network.CacheNetworkResponse
import coil3.toBitmap

internal const val THUMBNAIL_ETAG_HEADER = "ETag"

@OptIn(InternalCoilApi::class, ExperimentalCoilApi::class)
internal fun readThumbnailDiskEtag(
    imageLoader: ImageLoader,
    imageUrl: String,
): String? {
    val diskCache = imageLoader.diskCache ?: return null
    val snapshot = diskCache.openSnapshot(imageUrl) ?: return null
    return snapshot.use { entry ->
        try {
            diskCache.fileSystem.read(entry.metadata) {
                CacheNetworkResponse
                    .readFrom(this)
                    .headers[THUMBNAIL_ETAG_HEADER]
            }
        } catch (_: Exception) {
            null
        }
    }
}

internal fun imageContentHash(image: Image): String? =
    try {
        val width = image.width.coerceAtLeast(1)
        val height = image.height.coerceAtLeast(1)
        val sampleWidth = minOf(32, width)
        val sampleHeight = minOf(32, height)
        val bitmap = image.toBitmap(width = sampleWidth, height = sampleHeight)
        bitmapContentHash(bitmap)
    } catch (_: Exception) {
        null
    }

internal expect fun bitmapContentHash(bitmap: Bitmap): String

internal fun thumbnailContentChanged(
    baselineEtag: String?,
    freshEtag: String?,
    displayedContentHash: String?,
    validationImage: Image,
): Boolean {
    if (baselineEtag != null && freshEtag != null && baselineEtag != freshEtag) {
        return true
    }
    if (displayedContentHash == null) {
        return false
    }
    val validationHash = imageContentHash(validationImage) ?: return false
    return validationHash != displayedContentHash
}

internal fun invalidateThumbnailMemoryCache(
    imageLoader: ImageLoader,
    imageUrl: String,
) {
    imageLoader.memoryCache?.remove(coil3.memory.MemoryCache.Key(imageUrl))
}
