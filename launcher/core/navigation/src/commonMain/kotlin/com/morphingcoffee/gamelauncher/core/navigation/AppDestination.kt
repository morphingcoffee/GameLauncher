@file:OptIn(ExperimentalSerializationApi::class)

package com.morphingcoffee.gamelauncher.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Home : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data object Storage : AppDestination

    @Serializable
    data object Logs : AppDestination
}

val appNavigationConfig: SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed<AppDestination>()
                }
            }
    }
