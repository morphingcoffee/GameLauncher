package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.Manifest

sealed interface ManifestLoadResult {
    data class Success(
        val manifest: Manifest,
    ) : ManifestLoadResult

    data object DecodeFailed : ManifestLoadResult
}
