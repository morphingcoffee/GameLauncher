package com.morphingcoffee.gamelauncher.feature.home

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureHomeModule =
    module {
        viewModelOf(::HomeViewModel)
    }
