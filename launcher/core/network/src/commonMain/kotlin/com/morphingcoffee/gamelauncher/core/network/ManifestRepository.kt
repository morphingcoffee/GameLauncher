package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.Manifest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ManifestRepository(
    private val httpClient: HttpClient,
    private val manifestUrl: String = Config.manifestUrl,
    private val json: Json = manifestJson,
) {
    suspend fun fetchManifest(): Manifest =
        when (val result = loadManifest()) {
            is ManifestLoadResult.Success -> result.manifest
            ManifestLoadResult.DecodeFailed -> error("Failed to decode manifest")
            ManifestLoadResult.SkippedInDevBuild -> error("Manifest fetch skipped in dev builds")
        }

    suspend fun loadManifest(): ManifestLoadResult {
        val response = httpClient.get(manifestUrl)
        if (!response.status.isSuccess()) {
            error("Manifest fetch failed: ${response.status}")
        }
        val body = response.bodyAsText()
        return try {
            ManifestLoadResult.Success(json.decodeFromString(body))
        } catch (_: SerializationException) {
            ManifestLoadResult.DecodeFailed
        }
    }

    suspend fun fetchVersionIndex(url: String) =
        httpClient.get(url).body<com.morphingcoffee.gamelauncher.core.model.GameVersionIndex>()
}

private val manifestJson =
    Json {
        ignoreUnknownKeys = true
    }
