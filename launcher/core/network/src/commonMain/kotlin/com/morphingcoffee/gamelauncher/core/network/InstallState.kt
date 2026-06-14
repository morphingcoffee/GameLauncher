package com.morphingcoffee.gamelauncher.core.network

sealed interface InstallState {
    data object Unknown : InstallState

    data object NotInstalled : InstallState

    data class Installed(
        val version: String,
        val executablePath: String,
    ) : InstallState
}
