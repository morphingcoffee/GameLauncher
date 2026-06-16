package com.morphingcoffee.gamelauncher.di

import com.morphingcoffee.gamelauncher.core.network.networkModule
import com.morphingcoffee.gamelauncher.feature.home.featureHomeModule
import com.morphingcoffee.gamelauncher.feature.settings.featureSettingsModule
import org.koin.dsl.module

val appModule =
    module {
        includes(networkModule, featureHomeModule, featureSettingsModule)
    }
