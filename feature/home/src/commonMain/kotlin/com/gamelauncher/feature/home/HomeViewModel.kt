package com.gamelauncher.feature.home

import com.gamelauncher.core.architecture.MviViewModel

class HomeViewModel : MviViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState(),
) {
    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Started -> Unit
        }
    }
}
