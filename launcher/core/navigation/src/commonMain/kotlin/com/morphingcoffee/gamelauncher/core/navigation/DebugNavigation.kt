package com.morphingcoffee.gamelauncher.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DebugNavigation {
    private val _openLogsRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val openLogsRequests: SharedFlow<Unit> = _openLogsRequests.asSharedFlow()

    fun requestOpenLogs() {
        _openLogsRequests.tryEmit(Unit)
    }
}
