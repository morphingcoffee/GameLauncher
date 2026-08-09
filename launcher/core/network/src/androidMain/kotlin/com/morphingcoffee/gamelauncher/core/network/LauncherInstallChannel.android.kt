package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherRelease

actual object LauncherInstallChannel {
    actual suspend fun detect(): String? = null

    actual suspend fun resolveChannelKey(launcher: LauncherRelease?): String? = null
}
