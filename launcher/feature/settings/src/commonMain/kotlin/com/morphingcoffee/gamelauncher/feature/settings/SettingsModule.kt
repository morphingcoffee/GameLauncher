package com.morphingcoffee.gamelauncher.feature.settings

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureSettingsModule =
    module {
        viewModelOf(::StorageViewModel)
    }
