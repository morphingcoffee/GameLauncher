package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Extras
import coil3.PlatformContext
import coil3.getExtra
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.crossfade

val thumbnailRevalidateKey = Extras.Key(default = false)

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
    crossfadeMillis: Int = 300,
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
