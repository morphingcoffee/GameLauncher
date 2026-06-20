package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.InternalCoilApi
import coil3.network.CacheNetworkResponse

internal const val THUMBNAIL_ETAG_HEADER = "ETag"

@OptIn(InternalCoilApi::class, ExperimentalCoilApi::class)
internal fun hasDiskCacheEntry(
    imageLoader: ImageLoader,
    imageUrl: String,
): Boolean {
    val diskCache = imageLoader.diskCache ?: return false
    return diskCache.openSnapshot(imageUrl)?.use { true } ?: false
}

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

internal fun thumbnailDiskContentChanged(
    hadDiskEntry: Boolean,
    baselineEtag: String?,
    freshEtag: String?,
): Boolean {
    if (!hadDiskEntry) {
        return false
    }
    if (baselineEtag == null || freshEtag == null) {
        return false
    }
    return baselineEtag != freshEtag
}
