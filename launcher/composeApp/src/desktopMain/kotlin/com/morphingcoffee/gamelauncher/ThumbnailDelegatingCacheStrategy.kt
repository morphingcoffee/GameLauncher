package com.morphingcoffee.gamelauncher

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.thumbnailRevalidate

/**
 * Uses [CacheStrategy.DEFAULT] for normal thumbnail loads (instant disk/memory paint).
 * When [Options.thumbnailRevalidate] is set, issues a conditional GET using stored
 * ETag / Last-Modified metadata from Coil's disk cache.
 */
@OptIn(ExperimentalCoilApi::class)
internal class ThumbnailDelegatingCacheStrategy : CacheStrategy {
    private val default = CacheStrategy.DEFAULT

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): CacheStrategy.ReadResult {
        if (!options.thumbnailRevalidate) {
            return default.read(cacheResponse, networkRequest, options)
        }
        return revalidateRead(cacheResponse, networkRequest)
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult = default.write(cacheResponse, networkRequest, networkResponse, options)

    private fun revalidateRead(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
    ): CacheStrategy.ReadResult {
        val etag = cacheResponse.headers[ETAG_HEADER]
        val lastModified = cacheResponse.headers[LAST_MODIFIED_HEADER]
        if (etag == null && lastModified == null) {
            return CacheStrategy.ReadResult(networkRequest)
        }

        val headers = networkRequest.headers.newBuilder()
        etag?.let { headers[IF_NONE_MATCH_HEADER] = it }
        lastModified?.let { headers[IF_MODIFIED_SINCE_HEADER] = it }
        return CacheStrategy.ReadResult(networkRequest.copy(headers = headers.build()))
    }

    private companion object {
        const val ETAG_HEADER = "ETag"
        const val LAST_MODIFIED_HEADER = "Last-Modified"
        const val IF_NONE_MATCH_HEADER = "If-None-Match"
        const val IF_MODIFIED_SINCE_HEADER = "If-Modified-Since"
    }
}
