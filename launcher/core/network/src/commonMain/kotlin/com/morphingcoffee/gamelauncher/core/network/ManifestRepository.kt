package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.Manifest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ManifestRepository(
    private val httpClient: HttpClient,
    private val manifestUrl: String = Config.manifestUrl,
) {
    suspend fun fetchManifest(): Manifest = httpClient.get(manifestUrl).body()
}
