package com.gamelauncher.di

import com.gamelauncher.feature.home.featureHomeModule
import org.koin.dsl.module

val appModule = module {
    includes(featureHomeModule)
}
