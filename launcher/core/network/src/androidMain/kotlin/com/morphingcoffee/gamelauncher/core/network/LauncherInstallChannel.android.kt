package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherRelease

actual object LauncherInstallChannel {
    actual fun detect(): String? = null

    actual fun resolveChannelKey(launcher: LauncherRelease?): String? = null
}
