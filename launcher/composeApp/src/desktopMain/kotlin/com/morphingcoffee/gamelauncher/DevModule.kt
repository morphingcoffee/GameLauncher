package com.morphingcoffee.gamelauncher

import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import org.koin.dsl.module

val devModule =
    module {
        single<GameCatalogDataSource> { FakeGameCatalogDataSource(gameLauncher = get()) }
    }
