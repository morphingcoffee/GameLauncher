package com.morphingcoffee.gamelauncher.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameInstallRecord(
    @SerialName("game_id")
    val gameId: String,
    @SerialName("version")
    val version: String,
    @SerialName("executable_path")
    val executablePath: String,
    @SerialName("sha256")
    val sha256: String,
)
