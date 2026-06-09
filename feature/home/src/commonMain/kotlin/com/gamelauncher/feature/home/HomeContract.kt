package com.gamelauncher.feature.home

sealed interface HomeEvent {
    data object Started : HomeEvent
}

data class HomeState(
    val title: String = "Game Launcher",
    val subtitle: String = "Hello from Compose Multiplatform",
)

sealed interface HomeEffect
