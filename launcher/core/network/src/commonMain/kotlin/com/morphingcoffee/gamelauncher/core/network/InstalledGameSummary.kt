package com.morphingcoffee.gamelauncher.core.network

data class InstalledGameSummary(
    val gameId: String,
    val version: String,
    val sizeBytes: Long,
)
