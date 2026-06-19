package com.morphingcoffee.gamelauncher

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import okio.Path.Companion.toOkioPath
import java.io.File

@OptIn(ExperimentalCoilApi::class)
internal fun createImageLoader(
    context: PlatformContext,
    includeSlowNetwork: Boolean,
): ImageLoader {
    val coilHttpClient =
        HttpClient(CIO) {
            // Dedicated client for Coil image fetches (separate from manifest/download clients).
        }

    return ImageLoader
        .Builder(context)
        .diskCache {
            DiskCache
                .Builder()
                .directory(defaultCoilDiskCacheDirectory())
                .maxSizePercent(0.02)
                .build()
        }.components {
            if (includeSlowNetwork) {
                add(SlowNetworkImageInterceptor())
            }
            add(
                KtorNetworkFetcherFactory(
                    httpClient = { coilHttpClient },
                    cacheStrategy = { ThumbnailDelegatingCacheStrategy() },
                ),
            )
        }.build()
}

private fun defaultCoilDiskCacheDirectory(): okio.Path =
    File(System.getProperty("java.io.tmpdir"), "coil3_disk_cache").apply { mkdirs() }.toOkioPath()
