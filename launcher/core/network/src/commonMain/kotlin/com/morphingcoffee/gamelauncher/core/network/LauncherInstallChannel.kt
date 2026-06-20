package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherRelease

expect object LauncherInstallChannel {
    fun detect(): String?

    fun resolveChannelKey(launcher: LauncherRelease?): String?
}
