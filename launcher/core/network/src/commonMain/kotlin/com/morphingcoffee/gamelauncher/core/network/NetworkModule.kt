package com.morphingcoffee.gamelauncher.core.network

import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule =
    module {
        single { createHttpClient() }
        single(named("download")) { createDownloadHttpClient() }
        single { ManifestRepository(get()) }
        single { createGameLauncher() }
        single { createGameInstaller(get(named("download"))) }
        single<GameCatalogDataSource> { GameCatalogRepository(get(), get(), get()) }
    }
