package com.greenrou.kanata.features.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.features.details.content.AnimeDetailContent
import com.greenrou.kanata.features.details.model.AnimeDetailsEvent
import com.greenrou.kanata.core.сomposable.FavoriteIcon
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsScreen(
    animeId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEpisodeList: (VideoSource) -> Unit,
    viewModel: AnimeDetailsViewModel = koinViewModel(key = animeId.toString()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(animeId) {
        viewModel.handleEvent(AnimeDetailsEvent.LoadAnime(animeId))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AnimeDetailsEvent.NavigateBack -> onNavigateBack()
                is AnimeDetailsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is AnimeDetailsEvent.NavigateToEpisodeList -> onNavigateToEpisodeList(event.source)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.anime?.title ?: "Anime Detail") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(AnimeDetailsEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FavoriteIcon(
                        isFavorite = state.isFavorite,
                        onClick = { viewModel.handleEvent(AnimeDetailsEvent.ToggleFavorite) }
                    )
                },
                colors = if (state.coverFillsTopBar) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding),
                )
                state.anime != null -> AnimeDetailContent(
                    anime = state.anime!!,
                    videoSources = state.videoSources,
                    isSearching = state.isSearching,
                    onSourceClick = { viewModel.handleEvent(AnimeDetailsEvent.OpenEpisodeList(it)) },
                    topPadding = padding.calculateTopPadding(),
                    bottomPadding = padding.calculateBottomPadding(),
                    coverFillsTopBar = state.coverFillsTopBar,
                )
                state.error != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.handleEvent(AnimeDetailsEvent.LoadAnime(animeId)) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
