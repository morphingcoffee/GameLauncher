package com.morphingcoffee.gamelauncher

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Dev-only delay on Coil image fetches, including background thumbnail revalidation.
 */
class SlowNetworkImageInterceptor(
    private val minDelayMs: Long = 300,
    private val maxDelayMs: Long = 3_000,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        delay(Random.nextLong(minDelayMs, maxDelayMs + 1))
        return chain.proceed()
    }
}
