package com.morphingcoffee.gamelauncher.di

import com.morphingcoffee.gamelauncher.core.network.networkModule
import com.morphingcoffee.gamelauncher.core.telemetry.TelemetryPreferencesStore
import com.morphingcoffee.gamelauncher.core.telemetry.createTelemetryPreferencesStore
import com.morphingcoffee.gamelauncher.feature.home.featureHomeModule
import com.morphingcoffee.gamelauncher.feature.settings.featureSettingsModule
import org.koin.dsl.module

val appModule =
    module {
        single<TelemetryPreferencesStore> { createTelemetryPreferencesStore() }
        includes(networkModule, featureHomeModule, featureSettingsModule)
    }
