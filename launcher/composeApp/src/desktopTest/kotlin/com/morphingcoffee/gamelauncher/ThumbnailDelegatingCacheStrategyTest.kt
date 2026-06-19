package com.morphingcoffee.gamelauncher

import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.thumbnailRevalidateKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoilApi::class)
class ThumbnailDelegatingCacheStrategyTest {
    private val strategy = ThumbnailDelegatingCacheStrategy()

    @Test
    fun read_withoutRevalidateFlag_returnsCachedResponse() =
        runTest {
            val cacheResponse = networkResponse(etag = "\"v1\"")
            val networkRequest = NetworkRequest(url = URL)

            val result = strategy.read(cacheResponse, networkRequest, options(revalidate = false))

            assertEquals(cacheResponse, result.response)
            assertNull(result.request)
        }

    @Test
    fun read_withRevalidateFlag_addsConditionalHeaders() =
        runTest {
            val cacheResponse =
                networkResponse(
                    etag = "\"v1\"",
                    lastModified = "Wed, 21 Oct 2015 07:28:00 GMT",
                )
            val networkRequest = NetworkRequest(url = URL)

            val result = strategy.read(cacheResponse, networkRequest, options(revalidate = true))

            assertNull(result.response)
            assertEquals("\"v1\"", result.request?.headers?.get("If-None-Match"))
            assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", result.request?.headers?.get("If-Modified-Since"))
        }

    @Test
    fun read_withRevalidateFlag_andNoValidators_reusesNetworkRequest() =
        runTest {
            val cacheResponse = networkResponse()
            val networkRequest = NetworkRequest(url = URL)

            val result = strategy.read(cacheResponse, networkRequest, options(revalidate = true))

            assertEquals(networkRequest, result.request)
            assertNull(result.response)
        }

    private fun networkResponse(
        etag: String? = null,
        lastModified: String? = null,
    ): NetworkResponse {
        val headers =
            NetworkHeaders
                .Builder()
                .apply {
                    etag?.let { set("ETag", it) }
                    lastModified?.let { set("Last-Modified", it) }
                }.build()
        return NetworkResponse(headers = headers)
    }

    private fun options(revalidate: Boolean): Options =
        Options(
            context = coil3.PlatformContext.INSTANCE,
            extras =
                coil3.Extras
                    .Builder()
                    .apply {
                        if (revalidate) {
                            this[thumbnailRevalidateKey] = true
                        }
                    }.build(),
        )

    private companion object {
        const val URL = "https://cdn.example.com/assets/cool_game/thumbnail.webp"
    }
}
