package com.morphingcoffee.gamelauncher.di

import com.morphingcoffee.gamelauncher.feature.home.featureHomeModule
import org.koin.dsl.module

val appModule =
    module {
        includes(featureHomeModule)
    }
