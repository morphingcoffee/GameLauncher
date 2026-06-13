package com.morphingcoffee.gamelauncher.core.network

data class DownloadProgress(
    val gameId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
) {
    val fraction: Float
        get() =
            if (totalBytes <= 0L) {
                0f
            } else {
                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            }

    val percent: Int
        get() = (fraction * 100f).toInt()
}
