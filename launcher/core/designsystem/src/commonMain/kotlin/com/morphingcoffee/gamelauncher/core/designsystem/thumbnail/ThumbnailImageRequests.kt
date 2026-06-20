package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Extras
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.getExtra
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.crossfade

val thumbnailRevalidateKey = Extras.Key(default = false)

const val THUMBNAIL_CROSSFADE_MILLIS = 1_500

val ImageRequest.thumbnailRevalidate: Boolean
    get() = getExtra(thumbnailRevalidateKey)

val Options.thumbnailRevalidate: Boolean
    get() = getExtra(thumbnailRevalidateKey)

fun ImageRequest.Builder.thumbnailRevalidate(revalidate: Boolean = true): ImageRequest.Builder =
    apply {
        extras[thumbnailRevalidateKey] = revalidate
    }

fun buildThumbnailValidationRequest(
    context: PlatformContext,
    imageUrl: String,
    crossfadeMillis: Int = THUMBNAIL_CROSSFADE_MILLIS,
): ImageRequest =
    ImageRequest
        .Builder(context)
        .data(imageUrl)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .thumbnailRevalidate()
        .crossfade(crossfadeMillis)
        .build()

fun invalidateThumbnailMemoryCache(
    imageLoader: ImageLoader,
    imageUrl: String,
) {
    val memoryCache = imageLoader.memoryCache ?: return
    memoryCache.keys
        .filter { it.key == imageUrl }
        .forEach { memoryCache.remove(it) }
}
