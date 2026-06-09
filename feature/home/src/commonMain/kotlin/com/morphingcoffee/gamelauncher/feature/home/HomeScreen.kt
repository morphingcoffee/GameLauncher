package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.Started)
    }

    HomeScreenContent(state = state)
}

@Composable
fun HomeScreenContent(state: HomeState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = state.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(
    name = "Home — default",
    widthDp = 960,
    heightDp = 540,
    showBackground = true,
)
@Composable
private fun HomeScreenDefaultPreview() {
    LauncherTheme {
        HomeScreenContent(state = HomeState())
    }
}

@Preview(
    name = "Home — custom copy",
    widthDp = 960,
    heightDp = 540,
    showBackground = true,
)
@Composable
private fun HomeScreenCustomPreview() {
    LauncherTheme {
        HomeScreenContent(
            state = HomeState(
                title = "Game Launcher",
                subtitle = "Pick a game to install or play",
            ),
        )
    }
}
