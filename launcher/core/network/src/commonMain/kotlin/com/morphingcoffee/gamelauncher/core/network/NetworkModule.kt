package com.morphingcoffee.gamelauncher.core.network

import org.koin.dsl.module

val networkModule =
    module {
        single { createHttpClient() }
        single { ManifestRepository(get()) }
        single { createGameLauncher() }
        single { GameCatalogRepository(get(), get()) }
    }
