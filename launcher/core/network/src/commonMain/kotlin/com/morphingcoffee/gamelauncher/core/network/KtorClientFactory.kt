package com.morphingcoffee.gamelauncher.core.network

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
