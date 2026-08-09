package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherRelease

expect object LauncherInstallChannel {
    suspend fun detect(): String?

    suspend fun resolveChannelKey(launcher: LauncherRelease?): String?
}
