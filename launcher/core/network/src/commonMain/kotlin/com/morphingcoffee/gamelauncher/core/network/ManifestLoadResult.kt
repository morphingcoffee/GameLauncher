package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.Manifest

sealed interface ManifestLoadResult {
    data class Success(
        val manifest: Manifest,
    ) : ManifestLoadResult

    data object DecodeFailed : ManifestLoadResult

    /** Dev builds use fake catalog and must not fetch or evaluate prod update metadata. */
    data object SkippedInDevBuild : ManifestLoadResult
}
