package com.morphingcoffee.gamelauncher.core.model

/** Stable MSI product identity — keep in sync with composeApp/build.gradle.kts windows.upgradeUuid. */
object LauncherInstallIdentity {
    const val WINDOWS_MSI_DISPLAY_NAME_PROD = "GameLauncher"
    const val WINDOWS_MSI_DISPLAY_NAME_DEV = "GameLauncherDev"

    const val WINDOWS_MSI_UPGRADE_UUID_PROD = "8f2a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b"
    const val WINDOWS_MSI_UPGRADE_UUID_DEV = "9e3b2c4d-6f5e-7a81-0c9d-1e2f3a4b5c6d"
}
