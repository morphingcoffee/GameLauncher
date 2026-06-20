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
        single { createLauncherUpdateInstaller(get(named("download"))) }
        single { LauncherUpdateRepository(get(), get()) }
        single<GameCatalogDataSource> { GameCatalogRepository(get(), get(), get()) }
    }
